# SDK Görev Tanımı — BLE Yanıtından Sonuç Kodu Okuma

> Bu içerik mobil SDK projesine prompt olarak verilmek üzere hazırlanmıştır. Sunucu tarafı
> (`POST /api/v2/access` → yanıtta `code`) ve cihaz tarafı (BLE) **tamamlanmıştır**; bu çalışma
> SDK'nın cihazdan gelen kodu okuyup sözleşmeye dahil etmesidir.
>
> Ön koşul: **SDK Görev Tanımı** dokümanındaki kod listesi, ön-ek kuralları ve `code` alanının
> nullable tanımı burada da geçerlidir. Önce onu okuyun. Bu doküman yalnızca **BLE yolunu**
> anlatır.

---

## 1. Ne değişti

Cihaz artık geçiş sonucunu tek bir dışa dönük kodla ifade edebiliyor (`A-1001`, `B-2`,
`C-1234`, `A-9999` …) — sunucu yolundaki `code` alanıyla **aynı sözlük**.

Ancak bu kod **pazarlıksız gönderilmiyor.** Mevcut BLE yanıt paketinde `message` alanı uzunluk
öneki olmadan paketin sonuna kadar uzandığı için, kodu bu pakete sıkıştırmanın eski
uygulamaları bozmayan bir yolu yok. Bu yüzden cihaz kodu **yalnızca istemcinin anladığını
beyan ettiği durumda** gönderiyor.

Yani SDK tarafında iki iş var:

1. Cihazın yeni yanıtı destekleyip desteklemediğini **öğrenmek**,
2. Destekliyorsa yeni istek tipini gönderip yeni yanıtı **ayrıştırmak**.

Bu ikisi ayrılmaz. Beyan etmeden yeni istek göndermek sahada geçişleri kırar (bkz. Bölüm 4).

---

## 2. Akış — hangi adımda ne değişti

```
1. SDK  → karakteristiğe subscribe olur          (değişmedi)
2. cihaz→ challenge paketi notify eder            ★ sonuna 1 bayt eklendi
3. SDK  → challenge'ı çözüp write eder            ★ yeni istek tipi eklendi
4. cihaz→ sonuç paketi notify eder                ★ yeni yanıt tipi eklendi
```

Servis ve karakteristik UUID'leri değişmedi: `41726d6f-6e20-4f6e-6520-427572616461`.

---

## 3. Challenge paketi — yetenek baytı

Cihaz, subscribe anında bugünkü challenge paketini gönderiyor; **sonuna** bir yetenek baytı
eklendi.

| Offset | Uzunluk | İçerik                                  |
| ------ | ------- | --------------------------------------- |
| 0      | 1       | `0x02` — protokol sürümü                |
| 1      | 1       | `0x01` — GroupHeader.Auth               |
| 2      | 1       | `0x01` — Auth.PublicKeyChallenge        |
| 3      | 32      | deviceId (ASCII, tiresiz UUID)          |
| 35     | 32      | challenge                               |
| 67     | 16      | iv                                      |
| **83** | **1**   | **capabilities — YENİ, bulunmayabilir** |

Toplam: eskiden **83** bayt, yeni firmware'de **84** bayt.

`capabilities` bir bit alanıdır:

| Bit    | Anlam                                   |
| ------ | --------------------------------------- |
| `0x01` | Sonuç kodu destekleniyor (`ResultCode`) |

Diğer bitler gelecekteki yetenekler için ayrılmıştır; **bilinmeyen bitleri yok sayın**, paketi
reddetmeyin.

### Okuma kuralı

```
supportsResultCode = paket.length > 83 && (paket[83] & 0x01) != 0
```

Paket 83 bayt ise eski firmware'dir; kod alınamaz, bugünkü davranışa dönülür.

**Uzunluğu eşitlik ile kontrol etmeyin** (`length == 84` yazmayın). Sonraki yetenekler paketi
daha da uzatabilir; `> 83` ve bit maskesi kullanın.

`deviceId`, `challenge` ve `iv` alanlarının **offsetleri ve boyutları değişmedi**. Bu alanları
sabit offsetten okuyan mevcut kodunuz olduğu gibi çalışır.

### ⛔ Önce şunu teyit edin — tasarımın dayandığı tek varsayım

**Mevcut SDK `iv` alanını nasıl okuyor?**

| Okuma biçimi                                  | Sonuç                                               |
| --------------------------------------------- | --------------------------------------------------- |
| `iv = paket[67..83]` (sabit 16 bayt)          | ✅ Sorun yok, yetenek baytı görmezden gelinir       |
| `iv = paket[67..]` (**paketin sonuna kadar**) | ❌ **Kırılır** — 17 baytlık iv, doğrulama başarısız |

İkinci durumda yetenek baytı iv'ye karışır, challenge hesabı tutmaz ve **sahadaki tüm eski
sürümler geçiş yapamaz hâle gelir.** Bu, geriye dönük uyumluluğun tamamının dayandığı tek
varsayımdır.

Lehte bir gösterge var — AES-CBC 16 baytlık iv zorunlu kılar, 17 bayt verildiğinde crypto
katmanı hata verir, dolayısıyla sabit uzunluk okunuyor olması beklenir — ancak bu bir çıkarımdır,
kanıt değildir. **Firmware sahaya çıkmadan önce SDK ekibi bunu kod üzerinde teyit etmelidir.**

Aynı soru `deviceId` ve `challenge` için de geçerlidir, ancak ikisi de aralarında başka alan
bulunduğu için zaten sabit uzunlukla okunmak zorundadır; risk yalnızca **son alan** olan
`iv`'dedir.

Teyit olumsuzsa yetenek bildirimi paketin dışına taşınır (örneğin SDK'nın terminal firmware
sürümünü HUB'dan öğrenmesi); bu durumda haber verin, cihaz tarafı buna göre değiştirilir.

---

## 4. İstek — yeni tip `0x08`

Bugün SDK iki istek tipinden birini gönderiyor:

| `data[1]` | Tip                               |
| --------- | --------------------------------- |
| `0x06`    | MacfitChallenge                   |
| `0x07`    | MacfitChallengeWithInstallationId |

Yeni tip eklendi:

| `data[1]` | Tip                           | Gövde                       |
| --------- | ----------------------------- | --------------------------- |
| `0x08`    | MacfitChallengeWithResultCode | **`0x07` ile birebir aynı** |

`0x08`, `0x07`'nin gövdesini taşır ve tek fark olarak "ben genişletilmiş yanıtı anlıyorum"
beyanıdır.

### ⚠ `0x08` yalnızca yetenek baytı görüldüyse gönderilir

Bu maddenin ihlali sahada geçiş kaybettirir. Eski firmware `data[1]`'i `0x06` veya `0x07`
değilse isteği **"direction challenge"** olarak ayrıştırır: `memberId`'yi doğru offsetten okur
ama `hardwareId` beklediği yerde `barcodeId` bulur, `installationId` uzunluk baytını
`direction` sanır ve şifreli challenge'ı yanlış offsetten alır. Sonuç: doğrulama başarısız,
üye kapıda kalır.

Kural tek cümle: **yetenek baytı yoksa `0x06`/`0x07` gönderin.**

Sürümü tahmin etmeye, terminal listesinden çıkarım yapmaya veya "yeni SDK olduğuma göre cihaz
da yenidir" varsayımına gitmeyin. Tek geçerli sinyal, o bağlantıda gelen challenge paketidir.

### İstek gövdesi (`0x07` ve `0x08` için ortak)

| Offset  | Uzunluk  | İçerik                                           |
| ------- | -------- | ------------------------------------------------ |
| 0       | 1        | `0x02` — protokol sürümü                         |
| 1       | 1        | `0x06` / `0x07` / `0x08` — istek tipi            |
| 2       | 1        | `0xF0` Android, `0xF1` iOS                       |
| 3       | 16       | memberId (ASCII, `\0` ile sağdan doldurulmuş)    |
| 19      | 16       | barcodeId (ASCII, `\0` ile sağdan doldurulmuş)   |
| 35      | 16       | qrCodeId (UUID, ham 16 bayt)                     |
| 51      | 1        | dil — `0x00` Türkçe, `0x01` İngilizce            |
| 52      | 1        | installationId uzunluğu (yalnızca `0x07`/`0x08`) |
| 53      | değişken | installationId (yalnızca `0x07`/`0x08`)          |
| sonrası | değişken | encryptedChallenge                               |

---

## 5. Yanıt — yeni tip `0x09`

### Bugünkü yanıt (`0x06`/`0x07` isteğine) — DEĞİŞMEDİ

```
başarı : [0x02][0x01][0x03][0x21]
hata   : [0x02][0x01][0x03][0x20][reason][message…]
```

Bu paketler bit düzeyinde bugünkü halindedir ve öyle kalacaktır. Mevcut ayrıştırıcınıza
dokunmayın.

### Genişletilmiş yanıt (yalnızca `0x08` isteğine)

| Offset    | Uzunluk  | İçerik                                                  |
| --------- | -------- | ------------------------------------------------------- |
| 0         | 1        | `0x02` — protokol sürümü                                |
| 1         | 1        | `0x01` — GroupHeader.Auth                               |
| 2         | 1        | **`0x09`** — Auth.ChallengeResultWithCode               |
| 3         | 1        | `0x21` başarı / `0x20` hata                             |
| 4         | 1        | reason — sayısal `AccessResult`, eski alanla aynı       |
| 5         | 1        | codeLen — kod uzunluğu, en fazla `34`                   |
| 6         | codeLen  | code — ASCII (`A-1001`, `B-2`, `C-1234` …)              |
| 6+codeLen | değişken | message — UTF-8, **kesilmiş olabilir**; başarıda boştur |

### Ayrıştırırken dikkat edilecek üç nokta

**1. Alt-tip `0x03` değil `0x09`.** Genişletilmiş yanıtı ayrı bir dal olarak ayrıştırın;
mevcut `0x03` ayrıştırıcısının üzerine kurmayın. Yerleşimleri farklıdır.

**2. Başarıda da `reason` baytı vardır.** Eski formatta başarı paketi 4 bayttı ve `reason`
taşımıyordu; yeni formatta başarıda `reason = 0x0A` (10, Success) gelir. `0x21` görünce
paketin bittiğini varsaymayın.

**3. `message` kesilmiş olabilir, `code` kesilmez.** Kod bilinçli olarak mesajdan **önce**
yerleştirildi: MTU küçükse kırpılan mesaj olur, kod değil. Mesajın yarım gelebileceğini
varsayın ve kodu ondan bağımsız değerlendirin. Kodlar tek başına anlamlı olacak şekilde
tasarlanmıştır.

**Mesaj içeriği hakkında:** Red durumlarında mesaj **hiçbir zaman boş gelmez**. 3. parti kendi
gerekçesini bildirmişse onun metni aynen aktarılır; bildirmemişse cihaz sunucuyla **birebir aynı**
yerelleştirilmiş metni üretir (dil, istekteki dil baytından gelir). Yani aynı geçiş denemesi hangi
yoldan yanıtlanırsa yanıtlansın üyeye aynı cümleyi gösterir. Buna rağmen mesajı nihai kaynak
saymayın: gösterilecek metne **kod üzerinden** karar vermek, uygulamanın kendi metnini
kullanmasına ve ileride metin değiştiğinde uygulama güncellemesi gerekmemesine imkân verir.

---

## 6. Uyumluluk matrisi — dördü de sahada olacak

| SDK  | Cihaz | Ne olur                                                                                                    |
| ---- | ----- | ---------------------------------------------------------------------------------------------------------- |
| eski | eski  | Bugünkü davranış.                                                                                          |
| eski | yeni  | Challenge 84 bayt gelir, SDK son baytı yok sayar, `0x06`/`0x07` yazar → **yanıt bit bit bugünküyle aynı**. |
| yeni | eski  | Challenge 83 bayt, yetenek baytı yok → SDK `0x06`/`0x07` yazar, `code` **null** olur.                      |
| yeni | yeni  | Yetenek görülür → SDK `0x08` yazar → cihaz `0x09` ile kodu döner.                                          |

Uygulamalar cihazlardan hızlı güncellendiği için **üçüncü satır bir süre en yaygın kombinasyon
olacaktır.** `code == null` istisnai bir durum değil, normal işleyiştir; bu yolda bugünkü
davranışınızı (sayısal `reason` + `message`) aynen sürdürün.

---

## 7. Cihazdan gelebilecek kodlar

| Kod                | Anlamı                                                                 |
| ------------------ | ---------------------------------------------------------------------- |
| `A-1001`           | Kapı açıldı                                                            |
| `A-1002`           | `noAuth` QR — 3. parti doğrulaması yapılmadan kapı açıldı              |
| `A-2001`           | Üyenin barkodu var ama QR'ın Benefits yapılandırması yok               |
| `A-2002`           | PerfectGym reddetti                                                    |
| `B-<n>`            | Benefits reddetti — `n` Benefits'in kendi `statusCode` değeri          |
| `C-<n>` / `C-0000` | FlyBy reddetti — `n` FlyBy'ın kendi `code` değeri                      |
| `A-3001`           | Doğrulama başarılı ama kapı açılamadı                                  |
| `A-4001`           | BLE seviyesinde reddedildi — tanınmayan üye, geçersiz kimlik doğrulama |
| `A-5001`           | Benefits'e ulaşılamadı                                                 |
| `A-5002`           | FlyBy'a ulaşılamadı                                                    |
| `A-5003`           | PerfectGym'e ulaşılamadı                                               |
| `A-5004`           | Macfit CRM yönlendirmesine ulaşılamadı                                 |
| `A-6001`           | Yapılandırma eksik — QR, entegrasyon kimliği veya tanınmayan `crmType` |
| `A-9999`           | Sınıflanamayan                                                         |

Notlar:

- **`A-3002` cihaz yolundan gelmez.** Cihazın kendisi turnikedir; BLE bağlantısı
  kurulabildiyse cihaz ayaktadır.
- **`A-4001` burada BLE anlamını taşır.** Sunucuda "istek doğrulama katmanında reddedildi"
  demektir; cihazda "yerel kullanıcı listesinde bulunamadı veya challenge doğrulanamadı"
  demektir. Aynı kod, bağlama oturmuş anlam.
- **Çalışan (employee) fallback'i `A-1001` döner.** 3. parti servise ulaşılamadığında çalışan
  için kapı açılıyor; bu geçiş bugün başarı olarak raporlanır. Ayrım yalnızca sunucudaki adım
  loglarında görünür, SDK bunu ayırt edemez ve etmemelidir.

---

## 8. Kod listesi kapalı değildir

`B-` ve `C-` altındaki değerler 3. partinin kendi kodlarıdır ve **yorumlanmadan** ön-eklenir.
Cihaz `B-2` ile `B-7` arasında bir seçim yapmaz; Benefits ne döndüyse onu geçirir.

Bunun SDK için sonuçları:

- Listede olmayan bir `B-` / `C-` kodu gelebilir. **Bu bir hata değildir.** Bilinmeyen kodu
  düşürmeyin, `A-9999`'a çevirmeyin; ham haliyle taşıyın ve loglayın.
- 3. partinin değeri okunamadığında cihaz uydurma kod üretmez, `0000` kullanır (`C-0000`).
     Bunu "kod yok" değil, "3. parti kod vermedi" olarak ele alın.
- Kod biçimi: `<harf>-<en fazla 32 karakter>`, izin verilen karakterler `A-Z a-z 0-9 _ . -`.
  Toplam en fazla 34 karakter. Biçim doğrulaması yapıyorsanız bu kümeye göre yapın, bilinen
  kodların listesine göre değil.

---

## 9. Tetikleme tipi 3 ve 4 — iki kodun uzlaştırılması **SDK'nın işidir**

QR kodları dört tetikleme tipinden biriyle yapılandırılır:

| Değer | Anlam                         |
| ----- | ----------------------------- |
| 1     | Yalnızca Bluetooth            |
| 2     | Yalnızca uzaktan (sunucu)     |
| 3     | Önce Bluetooth, sonra uzaktan |
| 4     | Önce uzaktan, sonra Bluetooth |

3 ve 4'te tek bir geçiş denemesi için iki yol da çalışabilir ve SDK ardışık olarak **hem
cihazdan hem sunucudan** kod alabilir. Bu iki kodun kullanıcıya çelişkili anlatılmaması
gerekir.

**Bu uzlaştırma cihazda uygulanmadı; cihaz yalnızca kendi yolunun kodunu döner.** Karar SDK
tarafında verilecektir. Kararlaştırılan kural:

- **Son denenen yolun kodu geçerlidir.**
- Bir yol başarıyla sonuçlandıysa (`A-1001` / `A-1002`), diğer yolun kodu yok sayılır.

Örnek: cihaz Benefits'e ulaşamayıp `A-5001` döndü, ardından uzaktan yol denendi ve sunucu
Benefits'e ulaşıp `B-2` (günlük limit) aldı. Kullanıcıya gösterilmesi gereken `B-2`'dir.

---

## 10. Varsayım yapılmaması gerekenler

- **Yetenek baytının varlığını varsaymayın.** Her bağlantıda yeniden kontrol edin; aynı
  kullanıcı farklı terminallerde farklı firmware sürümleriyle karşılaşır.
- **Paket uzunluğunu eşitlikle kontrol etmeyin.** `> 83` ve bit maskesi.
- **`0x08`'i spekülatif göndermeyin.** Tek geçerli sinyal o bağlantıda gelen challenge'dır.
- **`0x09` yanıtını `0x03` ayrıştırıcısıyla okumaya çalışmayın.** Yerleşimleri farklıdır ve
  başarıda `reason` baytı vardır.
- **`code` alanını zorunlu yapmayın.** Nullable'dır ve bir süre çoğunlukla null gelecektir.
- **Bilinmeyen `B-`/`C-` kodunu normalize etmeyin.** Passthrough tasarımının amacı budur.
- **Sayısal `reason` alanını kaldırmayın.** Eski yolda tek bilgi kaynağıdır ve yeni yanıtta da
  yerinde durur; kod onun yerine değil, yanına gelir. Çelişki halinde `code` önceliklidir.

---

## 11. Kabul kriteri

Aşağıdaki beş senaryoda BLE üzerinden beş farklı kod elinize ulaşmalı:

| #   | Senaryo                            | Beklenen kod           |
| --- | ---------------------------------- | ---------------------- |
| 1   | Başarılı BLE geçişi                | `A-1001`               |
| 2   | Benefits günlük limit reddi        | `B-2`                  |
| 3   | FlyBy reddi                        | `C-<kod>` + mesajı     |
| 4   | Kapı açılamaması                   | `A-3001`               |
| 5   | Bir 3. parti servise ulaşılamaması | `A-5001`/`5002`/`5003` |

Ayrıca:

- **Yeni SDK, eski firmware'e karşı bugünkü davranışını değişmeden sürdürmelidir.** `code`
  null gelir, geçiş bugünkü gibi tamamlanır. Bu madde **fiziksel cihazda** doğrulanmalıdır;
  emülatör veya birim testi karşılamaz.
- Tetikleme tipi 3 veya 4 ile yapılandırılmış bir QR'da, iki yoldan farklı kodlar geldiğinde
  Bölüm 9'daki kuralın uygulandığı doğrulanmalıdır.

---

## Başlamadan önce netleştirin

| #   | Konu                                                                                 | Kim            |
| --- | ------------------------------------------------------------------------------------ | -------------- |
| 1   | `iv` sabit 16 bayt mı okunuyor, yoksa paketin sonuna kadar mı? (Bölüm 3)             | **SDK**        |
| 2   | Sahadaki en eski desteklenen SDK sürümü, yeni firmware'e karşı fiziksel cihazda test | SDK + Firmware |
| 3   | Tetikleme 3/4'te iki yolun uzlaştırılması SDK'da uygulanacak (Bölüm 9)               | SDK            |

1. madde olumsuz sonuçlanırsa mekanizma değişir; bu yüzden **ilk iş odur.** Diğer maddeler
   uygulamayı değil, yalnızca doğrulamayı ilgilendirir.

---

## Cihaz tarafındaki karşılıkları

Uygulamayı incelemek isterseniz (`app-macfit-terminal`):

| Ne                                      | Nerede                                                        |
| --------------------------------------- | ------------------------------------------------------------- |
| Opcode ve yetenek sabitleri             | `src/board/bluetooth/board.bluetooth.consts.ts`               |
| Challenge ve yanıt paketlerinin üretimi | `src/board/bluetooth/board.bluetooth.utils.ts`                |
| Akış ve kod seçimi                      | `src/board/bluetooth/board.bluetooth.accessCharacteristic.ts` |
| Kod listesi                             | `src/types/enums.ts` — `AccessResultCode`                     |
| Passthrough kural motoru                | `src/utils/utils.ts` — `buildPassthroughResultCode`           |

Belirsizlikte tahmin yürütmek yerine sorun. Sonuç kodları MAC+, SDK ve firmware arasında
paylaşılan bir sözleşmedir; tek taraflı yorum farkı sahada yanlış mesaj olarak görünür.
