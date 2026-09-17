# SDK → HUB & Cihaz Ekipleri — Sonuç Kodu Çalışması Geri Bildirimi

> `SDK_HUB_SERVER_PROMPT.md` ve `SDK_BLE_DEVICE_PROMPT.md` dokümanlarına karşılık hazırlanmıştır.
> SDK tarafındaki geliştirme tamamlandı; aşağıda (1) sorduğunuz soruların cevapları, (2)
> dokümanlarda düzeltilmesi gereken noktalar, (3) SDK'nın aldığı ve sizin bilmeniz gereken
> kararlar yer alıyor.

## Kimden ne bekliyoruz

| # | Konu | Kim | Aciliyet |
| - | ---- | --- | -------- |
| 1 | 408 yanıtında `message` gönderiminin sahadaki etkisi | HUB → MAC+ | Bilgilendirme |
| 2 | BLE istek gövdesi tablosunda offset 0 düzeltmesi | **Cihaz** | Doküman |
| 3 | Başarı paketinde `message` gönderilmesi | **Cihaz** | Firmware |
| 4 | ~~`installationId` boşken `0x08`~~ — cevaplandı (A); cihaz tarafında boş `installationId` → `undefined` normalizasyonu bekleniyor | **Cihaz** | **Test öncesi** |
| 5 | Uzlaştırma kuralının "kodsuz son yol" yorumu onayı | HUB + Cihaz | Teyit |
| 6 | BLE'de red mesajının boş gelip gelmeyeceği çelişkisi | HUB + Cihaz | Doküman |

---

## 1. Sorduğunuz soruların cevapları

### 1.1 Cihaz ekibine — `iv` sabit 16 bayt mı okunuyor? ✅ EVET, devam edebilirsiniz

`SDK_BLE_DEVICE_PROMPT.md` §3'teki "tasarımın dayandığı tek varsayım" maddesi **olumlu**.

Her iki platformda, hem sahadaki 2.2.1 sürümünde hem yeni sürümde alan sabit uzunlukla okunuyor:

```
deviceId  → 32 bayt (sabit)
challenge → 32 bayt (sabit)
iv        → 16 bayt (sabit)   ← "paketin sonuna kadar" DEĞİL
```

Ayrıca sahadaki sürümün alan listesi `iv`'den sonra bitiyor; 84 baytlık pakette 84. bayta hiç
ulaşmadan ayrıştırma sonlanıyor. Yani **uyumluluk matrisinin 2. satırı (eski SDK / yeni cihaz)
güvende** — eski uygulamalar yetenek baytını görmezden gelir, `0x06`/`0x07` yazmaya devam eder,
yanıt bit düzeyinde bugünküyle aynı kalır.

Yetenek baytı mekanizması paketin dışına taşınmak zorunda değil. Firmware sahaya çıkabilir.

### 1.2 HUB ekibine — Yayındaki sürümler `message`'ı 200 ve 408'de iletiyor mu?

**200 → Hayır. 408 → Evet.** Yani tablonuzdaki "Evet" satırı geçerli ve deploy öncesi karar
gerekiyor.

**200 (başarı) — risk yok.** Yayındaki sürümde `/access` yanıtının dönüş tipi iOS'ta boş bir
struct, Android'de `null` sınıfı; gövde ayrıştırılmadan atılıyor ve başarı geri bildirimi
mesajsız gönderiliyor. 200'de `message` göndermeniz sahadaki hiçbir uygulamayı etkilemez.

**408 (kapı açılamadı) — etkiler.** Mesaj uygulamaya **iki ayrı yoldan** ulaşıyor:

1. `PassFlowResult.message` — Bluetooth fallback'i kalmadığında (tetikleme tipi 2 ile
   yapılandırılmış tüm geçiş noktaları),
2. `PassFlowResult.states[].data` — **her durumda**, fallback olsa bile. Hata mesajı
   `RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT` adımının verisi olarak akış geçmişine yazılıyor ve
   bu liste uygulamaya açık.

Pratik sonucu: deploy anından itibaren, **uygulama güncellemesi olmadan**, bugüne kadar boş
metin alan yayındaki sürümler `"Kapı açılamadı. Lütfen tekrar deneyin; sorun sürerse kulüp
yetkilisine başvurun."` metnini almaya başlayacak. MAC+ "message doluysa onu göster" mantığı
kullanıyorsa 408 ekranındaki metin kendiliğinden değişir.

Bunun deploy'u engellemesi gerekmez: çökme, veri kaybı veya kod kaybı yok, gelen metin de zaten
kullanıcıya gösterilmek üzere yazılmış yerelleştirilmiş bir cümle. Bu bir **MAC+ bilgilendirmesi**.

Tek dikkat edilecek nokta: MAC+ `message`'ın dolu olup olmamasına göre **dallanıyorsa**, değişen
yalnızca metin olmaz, ekranın kendisi değişebilir — örneğin bugüne kadar "tekrar dene" ekranı
gösterilen yerde düz bir hata kutusu belirebilir. Yalnızca metni gösteriyorsa hiçbir etkisi yok.
MAC+ ekibinin 408 dalını bir kez gözden geçirmesi yeterli.

---

## 2. Dokümanlarda düzeltilmesi gereken noktalar

### 2.1 ✅ Cihaz — BLE §4 istek gövdesi tablosunda offset 0 yanlış (teyit edildi)

> Cihaz ekibi doğruladı: istek yolunda `data[0]` hiçbir yerde okunmuyor, `0x02` yalnızca cihazdan
> giden paketlere yazılıyor. SDK `0x01` yazmaya devam ediyor; **düzeltilecek olan doküman
> tablosudur.**

Tablo `offset 0 = 0x02 (protokol sürümü)` diyor. SDK oraya **`0x01` (GroupHeader.Auth)** yazıyor
ve pakete başka hiçbir şey eklemiyor:

```
gerçek:  [0x01][0x06|0x07|0x08][0xF0/0xF1][memberId 16][barcodeId 16][qrCodeId 16][dil 1]…
tabloda: [0x02][0x06|0x07|0x08][0xF0/0xF1][memberId 16][barcodeId 16][qrCodeId 16][dil 1]…
          ^^^^ tek fark
```

Diğer bütün offsetler (1, 2, 3, 19, 35, 51, 52, 53) tabloyla birebir tutuyor ve `0x06`/`0x07`
bugün sahada çalışıyor — dolayısıyla hatalı olan tablo, kod değil.

Dikkat çekici olan şu: **challenge ve yanıt** tablolarında offset 0 gerçekten `0x02` ve onlar
kodla uyuşuyor. Yani istek paketinde protokol baytı yok, cihazdan gelen paketlerde var. Bu
asimetri gerçek ve kasıtlı görünüyor, ama tabloya yansımamış. Düzeltilmezse bu tablodan yola
çıkan bir sonraki geliştirme bir bayt kaydırıp bütün alanları bozar.

Biz `0x08` için yalnızca 1. baytı değiştirdik, 0. bayta dokunmadık.

### 2.2 🔴 İkisi birden — BLE'de red mesajı boş gelir mi? Dokümanlar çelişiyor

| Nerede | Ne diyor |
| ------ | -------- |
| HUB §9 | "paket boyutu nedeniyle `message` alanına yer olmayabilir; **yalnızca `code` taşınabilir**" |
| HUB §10.4 | "Bu durumda `code` dolu, **`message` boş gelir**" |
| BLE §5 | "Red durumlarında mesaj **hiçbir zaman boş gelmez**" |

SDK ikisini de kaldırıyor, ama MAC+ emniyet supabını buna göre tasarlayacak: HUB'a inanırsa
kod→metin tablosunu zorunlu kılar, BLE dokümanına inanırsa `message`'a güvenir. BLE dokümanı daha
yeni ve daha spesifik; HUB §9'un güncellenmesi gerekiyor.

### 2.3 🟡 HUB — `result: 2 SUCCESS` her zaman `A-1001`/`A-1002` taşımıyor

HUB §10.3 tablosu `2 SUCCESS` satırına koşulsuz olarak "`A-1001` veya `A-1002`" yazıyor. Ama BLE
§6'nın 3. satırı (yeni SDK / eski cihaz) **başarılı** bir BLE geçişinde `code: null` üretiyor — ve
kendi ifadenizle bu kombinasyon bir süre en yaygın olan. Yani `SUCCESS` + `null` normal bir
durum. MAC+ başarı ekranında kodun dolu geleceğini varsayarsa kırılır; tablonun bunu söylemesi
gerekiyor.

### 2.4 🟢 HUB — §9 bayatlamış

"BLE yolunda hangi kodların üretileceği firmware geliştirmesi sırasında netleştirilecek" — artık
BLE §7'de tam liste var. Aynı paragraf 2.2'deki çelişkiyi de içeriyor; komple güncellenmeli.

---

## 3. SDK'nın aldığı kararlar

### 3.1 Uzlaştırma — "kodsuz son yol öncekini silmez" (teyit bekliyoruz)

Her iki doküman da diyor ki: *"Her iki yol da başarısızsa son denenen yolun kodu geçerlidir."*
Dokümanlar, **son yolun hiç kod üretmediği** durumu tanımlamıyor. SDK şöyle yorumladı:

> Kod üreten son yol kazanır. Kod üretmeyen bir yol, önceki yolun kodunu **silmez**.

Sizin belirttiğiniz sıralamada (her durumda önce BLE, sonra sunucu) bu yorum ile kuralın lafzı
**tek bir senaryo dışında aynı sonucu veriyor**:

| Senaryo | Lafzen | SDK'nın uyguladığı |
| ------- | ------ | ------------------ |
| BLE başarısız (kodsuz, eski firmware) → sunucu kodla başarısız | sunucu kodu | sunucu kodu ✓ aynı |
| BLE başarısız (kodlu) → sunucu kodla başarısız | sunucu kodu | sunucu kodu ✓ aynı |
| BLE veya sunucu başarılı | başarılı yolun kodu | başarılı yolun kodu ✓ aynı |
| BLE kodla başarısız → **sunucuya hiç ulaşılamadı** (ağ hatası/timeout) | `null` | **cihazın kodu** |

Son satırın gerekçesi: sunucuya hiç gidilmediği için ortada sunucu kodu yok; cihazın ürettiği
kod (örneğin `A-5001`) elimizdeki tek gerçek bilgi ve `null`'dan kesinlikle daha faydalı. Aynı
mantık tetikleme tipi 4 için de geçerli.

Bu yorumu onaylıyor musunuz? Onaylamazsanız tek satırlık değişiklik.

### 3.2 Dil — normalize edildi, sunucu ve cihaz artık ayrışamaz

Bir uyumsuzluk bulduk ve düzelttik. Dil değeri hiçbir yerde normalize edilmiyordu:

| Uygulamanın verdiği | `Accept-Language` → sunucu | BLE dil baytı → cihaz |
| ------------------- | -------------------------- | --------------------- |
| `"en"` | İngilizce | `0x01` İngilizce ✓ |
| `"en-US"` | İngilizce | **`0x00` Türkçe** ✗ |
| `"EN"` | İngilizce | **`0x00` Türkçe** ✗ |

Yani BLE §5'in "aynı geçiş denemesi hangi yoldan yanıtlanırsa yanıtlansın üyeye aynı cümleyi
gösterir" vaadi, tam eşleşme dışındaki her değerde bozuluyordu. SDK artık sunucunun kuralını
birebir uyguluyor (`tr` ile başlıyorsa Türkçe, diğer her durumda İngilizce) ve hem başlığı hem
BLE baytını aynı kaynaktan besliyor. **Sizden bir şey gerekmiyor**, bilginize.

### 3.3 Başarı mesajı — cihaz tarafında eksik

Uzaktan yolda başarı yanıtındaki `message` artık uygulamaya iletiliyor
(`"Kapı açıldı, geçebilirsiniz."`). BLE yolunda ise BLE §5 gereği başarıda mesaj gelmiyor.

Sonuç: aynı üye aynı turnikeden geçtiğinde, uzaktan yoldan geçerse metin var, BLE'den geçerse
yok. İki kanalın eşitlenmesi için **cihazın da başarı paketinde `message` göndermesi**
gerekiyor.

SDK tarafı hazır: başarı paketindeki `message` alanı zaten okunuyor ve uygulamaya taşınıyor.
Firmware göndermeye başladığı anda **SDK güncellemesi gerekmeden** çalışacak.

### 3.4 ✅ ÇÖZÜLDÜ — `0x08` artık `installationId` olmadan da gönderiliyor

Cihaz ekibi teyit etti: `0x08` ayrıştırıcısı `0x07` ile aynı dalı kullanıyor, uzunluk baytını
okuyup `0` değerini kabul ediyor. SDK güncellendi — yetenek baytı görüldüğü **her** durumda
`0x08` gönderiliyor, `installationId` boşsa uzunluk `0x00` yazılıyor ve şifreli challenge
`[53]`'ten başlıyor.

Opcode ile gövde şekli tek bir kaynaktan türetiliyor, yani ikisinin ayrışması yapısal olarak
mümkün değil. Eski firmware'de davranış bit düzeyinde aynı: yetenek baytı yoksa `0x07`/`0x06`.

**Cihaz tarafında kalan ön koşul:** uzunluk `0` geldiğinde `installationId` boş string (`""`)
yerine `undefined` olarak normalize edilecek. Bu yapılmadan `0x08` kullanılırsa Benefits
`registerVisit` gövdesine `"installationId": ""` gider ve Benefits bunu "eksik" değil "geçersiz"
sayabilir. **Bu normalizasyon, yetenek baytını yayınlayan her firmware sürümünde bulunmalıdır** —
aksi hâlde `installationId` tanımsız kurulumlar bugün geçebilirken geçemez hâle gelir.

Aşağıdaki madde tarihsel kayıt olarak bırakıldı.

<details>
<summary>Sorunun özgün tanımı</summary>

#### `0x08` yalnızca `installationId` varken gönderiliyordu

BLE §4 `0x08`'in gövdesini "`0x07` ile birebir aynı" olarak tanımlıyor ve gövde tablosunda
`installationId` uzunluk baytını "yalnızca `0x07`/`0x08`" diye işaretliyor. `0x07` bugün yalnızca
`installationId` doluyken gönderiliyor; boşken `0x06` gönderiliyor ve `0x06`'nın gövdesinde o
alanlar hiç yok.

Doküman **`installationId` boşken `0x08` gönderilip gönderilemeyeceğini tanımlamıyor.** Sıfır
uzunluklu bir `installationId` ile gönderilebilir gibi duruyor, ama tahmin yürütmedik: SDK şu an
yalnızca `0x07` göndereceği durumlarda `0x08`'e geçiyor.

**Pratik sonucu — test kurulumu için kritik:** Uygulama `installationId` olmadan yapılandırılırsa
BLE yolunda `0x06` gönderilir ve **kod hiçbir zaman gelmez**. Aynı şey `barcode` boşken de
geçerli: o durumda "direction challenge" yoluna giriliyor ve o yolun kod varyantı yok. Yani uçtan
uca testte:

> Test uygulaması hem `barcode` hem `installationId` ile yapılandırılmış olmalı. Aksi hâlde BLE
> kodları hiç görünmez ve bu, özelliğin çalışmadığı gibi görünür.

Cihaz ekibinden beklentimiz: `installationId` boşken `0x08` gönderilebilir mi? Gönderilebilirse
tek satırlık değişiklikle o durumu da kapsarız.

</details>

### 3.5 Sayısal `reason` uygulamaya ulaşmıyor — beklentiniz karşılanmıyor olabilir

BLE §6 "bu yolda bugünkü davranışınızı (sayısal `reason` + `message`) aynen sürdürün" diyor, §10
ise "sayısal `reason` alanını kaldırmayın".

SDK `reason`'ı ayrıştırıyor ve dahili olarak taşıyor, ancak **uygulamaya iletmiyor** — MAC+ yalnızca
`message`'ı görüyor. Bu bu çalışmadan önce de böyleydi, yani bir gerileme değil; ama doküman
SDK'da olmayan bir yeteneği varsayıyor. `PassFlowResult`'a `reason` da eklenmesini istiyorsanız
söyleyin; bizim görüşümüz `code`'un zaten onun yerini aldığı yönünde.

---

### 3.6 `barcode` boş kurulumlar — kapsam dışı (teyit edildi)

`barcode` boşken SDK direction challenge (`0x05`) yoluna giriyor ve o yolun sonuç kodu varyantı
yok. Cihaz ekibi bunun bilinçli bir kapsam sınırı olduğunu, ihtiyaç netleşirse `0x05` için de bir
opcode (örn. `0x0A`) eklemenin düşük maliyetli olduğunu belirtti. Şimdilik bu kurulumlarda BLE'den
sonuç kodu gelmeyeceğini kabul ediyoruz.

---

## 4. SDK tarafında tamamlananlar

- `/access` yanıtındaki `code`, HTTP durum kodundan bağımsız olarak okunuyor (200, 401, 403, 404,
  408, 500 — hepsinde) ve `PassFlowResult.code` ile uygulamaya iletiliyor
- BLE yolunda yetenek baytı her bağlantıda yeniden okunuyor, yalnızca görüldüğünde `0x08`
  gönderiliyor, `0x09` yanıtı ayrı bir dalda ayrıştırılıyor; mevcut `0x03` ayrıştırıcısına
  dokunulmadı
- Aynı `code` alanı iki kanalı da besliyor; MAC+ kodun hangi yoldan geldiğini bilmek zorunda değil
- `result: Int` değişmedi, `code` onun yanına eklendi, nullable
- Bilinmeyen `B-`/`C-` kodları ham haliyle taşınıyor; SDK hiçbir normalizasyon veya biçim
  doğrulaması yapmıyor
- Ayrıştırıcı gerçek paket baytlarıyla test edildi: 83/84/85 baytlık challenge, eski ve yeni hata
  paketi, başarı paketi, MTU nedeniyle kırpılmış mesaj, `codeLen = 0` ve paketten uzun bozuk
  `codeLen`

**Kalan:** fiziksel cihazda doğrulama — özellikle BLE §11'in şart koştuğu "yeni SDK / eski
firmware" kombinasyonu.
