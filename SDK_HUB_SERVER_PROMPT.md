# SDK Görev Tanımı — `/access` Yanıtlarındaki Sonuç Kodları

> Bu içerik SDK projesine prompt olarak verilmek üzere hazırlanmıştır. Sunucu tarafındaki
> geliştirme tamamlanmış ve doğrulanmıştır; aşağıdaki sözleşme bağlayıcıdır.

---

## Bağlam

Armon HUB sunucusunun `POST /api/v2/access` ucu, bugüne kadar hata durumlarında yalnızca serbest
metin bir `message` alanı dönüyordu. MAC+ mobil uygulaması bu metni doğrudan kullanıcıya
gösteriyordu; kendi diline çeviremiyor, duruma göre farklı aksiyon alamıyordu.

Sunucu tarafında artık **her yanıta makine tarafından işlenebilir bir `code` alanı** eklendi.
SDK'nın görevi bu kodu mobil uygulamaya taşımak, MAC+'ın da koda göre kendi metnini ve aksiyonunu
seçebilmesini sağlamak.

**Değişiklik eklemelidir.** Hiçbir alan kaldırılmadı, hiçbir HTTP durum kodu değişmedi. Kodu
tanımayan mevcut SDK sürümleri etkilenmez.

---

## 1. Yanıt sözleşmesi

Tüm `/api/v2/access` yanıtları artık şu gövdeyi taşır:

```json
{
  "code": "B-2",
  "message": "Günlük giriş hakkı limitini doldurdun."
}
```

| Alan      | Tip    | Zorunlu | Açıklama                                                             |
| --------- | ------ | ------- | -------------------------------------------------------------------- |
| `code`    | string | Evet    | Sonuç kodu. Aşağıdaki listeden bir değer ya da passthrough kalıbı.   |
| `message` | string | Hayır   | Kullanıcıya gösterilebilir metin. Başarı yanıtlarında da gönderilir. |

### Önceki davranışa göre fark

| Durum                | Önce                                   | Şimdi                                |
| -------------------- | -------------------------------------- | ------------------------------------ |
| Başarı (200)         | Boş gövde                              | `{ code, message }`                  |
| Kapı açılamadı (408) | Boş gövde                              | `{ code, message }`                  |
| Üye reddi (401)      | `{ message }`                          | `{ code, message }` — **metin aynı** |
| Hata (500)           | `{ message: "Server Error - E-1003" }` | `{ code, message }` — anlamlı metin  |

### ⚠ Cevabını sizden beklediğimiz soru

Tablodaki ilk iki satır tek gerçek geriye dönük uyumluluk riskini taşıyor: **200 ve 408
yanıtları eskiden boş gövde dönüyordu, artık `message` alanı da geliyor.**

Bu, geliştireceğiniz yeni sürümü değil, **sahada hâlihazırda çalışan SDK sürümlerini**
ilgilendiriyor — sunucu değişikliği canlıya çıktığı anda onlar da bu gövdeyi almaya başlayacak.
Eski bir sürüm `message` alanını "doluysa uygulamaya ilet / göster" mantığıyla işliyorsa, daha
önce hiçbir metin göstermediği **başarı** ekranında artık _"Kapı açıldı, geçebilirsiniz."_
görünmeye başlar.

**Lütfen kontrol edip bize bildirin:**

> Mevcut (yayındaki) SDK sürümleri, `/access` yanıtındaki `message` alanını **başarı (200)** ve
> **zaman aşımı (408)** durumlarında okuyup uygulamaya iletiyor mu?

| Cevap                                          | Sonuç                                                                                                              |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Hayır**, yalnızca hata durumlarında okunuyor | Geriye dönük uyumluluk tamdır, ek işlem yok                                                                        |
| **Evet**                                       | HUB tarafında karar gerekir (örneğin 200 yanıtında `message` gönderilmemesi). **Deploy öncesi bilmemiz gerekiyor** |

Bu sorunun cevabı HUB'ın canlıya çıkış kararını etkilediği için, geliştirmeye başlamadan önce
yanıtlanmasını rica ediyoruz.

---

## 2. Kod listesi

### Bizim kodlarımız (`A-` ön-eki)

| Kod      | HTTP      | Anlam                                                          | Beklenen istemci aksiyonu                                                                   |
| -------- | --------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `A-1001` | 200       | Kapı açıldı                                                    | Başarı ekranı                                                                               |
| `A-1002` | 200       | Kapı açıldı — doğrulamasız QR (`noAuth`)                       | Başarı ekranı. Kullanıcıya `A-1001` ile aynı görünür; ayrım analitik içindir                |
| `A-2001` | 401       | Geçiş noktası MultiSport/Benefits kapsamında değil             | Sabit mesaj göster                                                                          |
| `A-2002` | 401       | PerfectGym geçişi reddetti                                     | `message`'ı göster. PerfectGym kendi kodunu döndürmediği için kod bizden, metin ondan gelir |
| `A-3001` | 408       | Doğrulama başarılı, ancak hiçbir turnike kapıyı açamadı        | "Tekrar dene"                                                                               |
| `A-3002` | 404       | Turnike tanımlı ama sunucuya bağlı değil, deneme hiç yapılmadı | "Başka turnikeyi dene"                                                                      |
| `A-4001` | 403 / 400 | İstek doğrulama katmanında reddedildi, akış hiç başlamadı      | Aşağıdaki nota bakınız                                                                      |
| `A-5001` | 500       | Benefits servisine ulaşılamadı                                 | "Kısa süre sonra tekrar dene"                                                               |
| `A-5002` | 500       | FlyBy servisine ulaşılamadı                                    | "Kısa süre sonra tekrar dene"                                                               |
| `A-5003` | 500       | PerfectGym servisine ulaşılamadı                               | "Kısa süre sonra tekrar dene"                                                               |
| `A-5004` | 500       | CRM yönlendirme servisine ulaşılamadı                          | "Kısa süre sonra tekrar dene"                                                               |
| `A-6001` | 500       | Geçiş noktası konfigürasyonu eksik/hatalı                      | Kulüp yetkilisine yönlendir. **Tekrar denemek işe yaramaz**                                 |
| `A-9999` | 500 / 404 | Sınıflanamayan hata (fallback)                                 | Genel hata + destek                                                                         |

**`A-4001` hakkında:** İstek gövdesi hatalıysa **400**, diğer tüm durumlarda **403** döner. Bugün
yalnızca şu iki durumda üretilir: üye kimliği eksik veya `"0"`, ve izin verilmeyen `User-Agent`.

### Passthrough kodları — numarayı sunucu üretmez

3. parti servislerin kendi kodları **yorumlanmadan**, yalnızca ön-eklenerek aktarılır. Bu sayede
   Benefits veya FlyBy yeni bir hata kodu eklediğinde sunucuda geliştirme gerekmez — **ve SDK/MAC+
   tarafında da daha önce görülmemiş bir kod gelebilir.**

**`B-<statusCode>` — Benefits / MultiSport, HTTP 401**

| Kod      | Anlam                                          |
| -------- | ---------------------------------------------- |
| `B-1`    | Kart doğrulanamadı                             |
| `B-2`    | Günlük giriş limiti dolu                       |
| `B-3`    | Kart başka bir cihaza kayıtlı                  |
| `B-5`    | Kart bloke                                     |
| `B-23`   | Benefits sunucu hatası bildirdi                |
| `B-9999` | Benefits bilinmeyen hata bildirdi              |
| `B-<n>`  | **Listede olmayan herhangi bir kod gelebilir** |
| `B-0000` | Sözleşmede rezerve; mevcut akışta üretilmiyor  |

**`C-<code>` — FlyBy, HTTP 401**

| Kod      | Anlam                                                 |
| -------- | ----------------------------------------------------- |
| `C-1001` | Örnek: "Kullanıcı blacklist kaydında bulunmaktadır."  |
| `C-1019` | Örnek: "Üyelik tipinin giriş/çıkış saatleri dışında…" |
| `C-<n>`  | **Listede olmayan herhangi bir kod gelebilir**        |
| `C-0000` | FlyBy reddetti ama okunabilir bir kod göndermedi      |

`C-` kodlarında `message` doğrudan FlyBy'dan geldiği gibi aktarılır; sunucu çeviri yapmaz.

**Kod biçimi garantisi:** `<tek harf>-<en fazla 32 karakter>`. Numara kısmı `A-Z a-z 0-9 _ . -`
karakterlerinden oluşur; sunucu bunu garanti eder, ayrıştırılamayan bir değer geldiğinde `0000`
kullanılır. Yani kodun tamamı en fazla 34 karakterdir ve alfanümerik olabilir (`C-E1019` gibi).

---

## 3. Kritik tasarım kuralı — ön-ek bazlı varsayılan

**Eşleme tablosunu kodun tamamına göre değil, ön-ek harfine göre varsayılanlı kurun.**

Bu isteğe bağlı bir öneri değil, sözleşmenin çalışması için gereken şart. Sebebi: passthrough
tasarımı gereği daha önce görmediğiniz `B-` ve `C-` kodları gelebilir, ve sunucu ileride
`A-9999` kovasından yeni kodlar çıkarabilir. Ön-ek varsayılanı yerindeyse bu durumların hiçbiri
uygulamayı kırmaz.

```
A-1xxx  → başarı
A-2xxx  → erişim reddi          → mesajı göster
A-3xxx  → kapı / turnike        → "tekrar dene" veya "başka turnike"
A-4xxx  → istemci / oturum      → oturumu yenile, bir kez tekrar dene
A-5xxx  → servis iletişimi      → "kısa süre sonra tekrar dene"
A-6xxx  → konfigürasyon         → kulüp yetkilisine yönlendir
A-9999  → sınıflanamayan        → genel hata + destek
B-*     → MultiSport reddi      → bilinen kodda özel metin, bilinmeyende `message`
C-*     → FlyBy reddi           → `message`
```

**Bilinmeyen bir kod geldiğinde:** ön-ek varsayılanına düş ve `message` alanını göster.
Asla "bilinmeyen kod" gibi bir hata ekranı gösterme; sunucu her zaman gösterilebilir bir metin
gönderir.

---

## 4. `message` alanı neden kalıcı

Geçici bir uyumluluk alanı değildir. Passthrough tasarımının doğrudan sonucudur: FlyBy veya
Benefits bize haber vermeden yeni bir kod eklediğinde, MAC+ tablosunda o kodun karşılığı
olmayacaktır. O anda kullanıcıya anlamlı bir şey gösterebilmenin tek yolu `message`'dır.

MAC+ kendi metin tablosunu kurduktan sonra da bu alan emniyet supabı olarak kalmalıdır.

---

## 5. Dil

Sunucu mesaj dilini **yalnızca `Accept-Language` başlığından** okur:

- `tr` ile başlıyorsa → Türkçe
- Diğer her durumda → İngilizce

SDK bu başlığı göndermeye devam etmelidir. `C-` kodlarında metin FlyBy'dan geldiği için dil
kontrolü sunucuda değildir.

---

## 6. Mesaj metinleri (varsayılan)

MAC+ kendi tablosunu kurana kadar sunucunun döndüğü metinler. Genel mesaj:
TR `Geçiş kontrolü sırasında bir hata oluştu.` / EN `An error occurred during the access attempt.`

| Kod                 | TR                                                                                                                          | EN                                                                                                                          |
| ------------------- | --------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `A-1001` / `A-1002` | Kapı açıldı, geçebilirsiniz.                                                                                                | The door is open, you can go through.                                                                                       |
| `A-2001`            | MultiSport Plus üyeliğinle yalnızca MACFit kulüplerine giriş yapabilirsin.                                                  | You can only enter MACFit clubs with your MultiSport Membership.                                                            |
| `A-2002`            | PerfectGym'in mesajı, aynen                                                                                                 | PerfectGym'in mesajı, aynen                                                                                                 |
| `A-3001`            | Kapı açılamadı. Lütfen tekrar deneyin; sorun sürerse kulüp yetkilisine başvurun.                                            | We couldn't open the door. Please try again; if the problem continues, contact club staff.                                  |
| `A-3002`            | Bu turnike şu anda kullanılamıyor. Lütfen başka bir turnikeyi deneyin.                                                      | This gate is currently unavailable. Please try another gate.                                                                |
| `A-4001`            | Oturumunuz doğrulanamadı. Lütfen tekrar deneyin.                                                                            | We couldn't verify your session. Please try again.                                                                          |
| `A-5001` … `A-5004` | Üyeliğiniz şu anda doğrulanamıyor. Lütfen kısa bir süre sonra tekrar deneyin.                                               | Membership verification is temporarily unavailable. Please try again shortly.                                               |
| `A-6001`            | Bu geçiş noktasında bir sorun oluştu. Lütfen kulüp yetkilisine başvurun.                                                    | There's a problem with this entry point. Please contact club staff.                                                         |
| `A-9999`            | Bir hata oluştu. Lütfen kulüp yetkilisine başvurun.                                                                         | Something went wrong. Please contact club staff.                                                                            |
| `B-1`               | Kartın doğrulanamadı. Sorunu çözmek için lütfen MultiSport ile iletişime geç.                                               | We couldn't verify your card. Please contact MultiSport to resolve the issue.                                               |
| `B-2`               | Günlük giriş hakkı limitini doldurdun.                                                                                      | You've reached your daily visit limit.                                                                                      |
| `B-3`               | Kartınız farklı bir cihaza kayıtlı olduğu için kulüp girişi yapılamıyor. Destek için lütfen MultiSport ile iletişime geçin. | Your card is registered to a different device, so club entry cannot be completed. Please contact MultiSport for assistance. |
| `B-5`               | Kartınız bloke edildiği için kulüp girişi yapılamıyor. Destek için lütfen MultiSport ile iletişime geçin.                   | Your card is blocked, so club entry cannot be completed. Please contact MultiSport for assistance.                          |
| `B-23`              | Bir sunucu hatası oluştu. Lütfen tekrar deneyin.                                                                            | A server error occurred. Please try again.                                                                                  |
| `B-9999`            | Bir hata oluştu. Lütfen destek ekibiyle iletişime geçin.                                                                    | Something went wrong. Please contact support.                                                                               |
| `C-<n>`             | FlyBy'ın mesajı, aynen                                                                                                      | FlyBy'ın mesajı, aynen                                                                                                      |

**Yönlendirme kuralı:** `A-` kodlarında kullanıcı **kulüp yetkilisine**, `B-` kodlarında
**MultiSport'a** yönlendirilir. `C-` kodlarında yönlendirmeyi FlyBy'ın metni belirler.

---

## 7. Diğer uçlar

**Sonuç kodu sözleşmesi `/api/v2/access` ucuna aittir.** Diğer uçlarda kod yalnızca genel hata
katmanından geçen yanıtlarda bulunur; her yanıtta kod beklemeyin.

`code` **bulunan** durumlar:

- Şema doğrulama hatası → `A-4001`, HTTP 400 (her `/api/` ucu için geçerli)
- Tanımsız uç nokta → `A-9999`, HTTP 404 — yalnızca `Accept: application/json` gönderildiğinde;
  aksi hâlde sunucu HTML döner

`code` **bulunmayan** durumlar (bilerek; bu uçlar erişim akışının parçası değildir):

| Uç                                | Yanıt                                                          |
| --------------------------------- | -------------------------------------------------------------- |
| `/api/v2/analytics`               | Başarı `200 {}`, hata `500 { message }` — kod yok              |
| `/api/v2/listAccessPointsRequest` | Başarı: liste, yeniden senkron gerektiğinde `409 {}` — kod yok |
| `/api/v2/sdk/handshake`           | Bölüm 8'e bakınız — kod yok                                    |

Bu uçların mevcut yanıt şekilleri **değişmedi**; onlar için bir uyarlama yapmanız gerekmiyor.

---

## 8. DEĞİŞMEYENLER — bunlar için geliştirme yapmayın

Bu maddeler kasıtlı olarak kapsam dışı bırakıldı ve **mevcut davranışlarını aynen koruyor**.
Ayrı bir handshake/güvenlik çalışmasında bütüncül olarak ele alınacaklar.

| Konu                                    | Mevcut davranış                                                                                                                                                                   |
| --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST /api/v2/sdk/handshake`            | Yanıt gövdesinde `code` **yok**. Reddedilen handshake istekleri **hiç yanıt döndürmüyor**; istemci kendi timeout'unu bekliyor. Bu bilinen bir durumdur, o çalışmada düzeltilecek. |
| İmza doğrulama (`mobilepass-signature`) | Geçersiz imza, süresi dolmuş timestamp, replay ve eksik oturum durumlarında istek **reddedilmiyor, geçiriliyor**.                                                                 |

Bu iki konuda SDK tarafında şu an bir değişiklik beklenmiyor. `A-4001` kodunu handshake
yanıtlarında aramayın.

---

## 9. BLE akışı için not

Aynı geçiş akışı cihazla doğrudan BLE üzerinden de işletiliyor. O yolda paket boyutu nedeniyle
`message` alanına yer olmayabilir; yalnızca `code` taşınabilir.

Bu, MAC+'ın kod → metin tablosunu kendi tarafında kurmasını **er geç zorunlu** kılıyor.
Mimariyi kurarken metnin sunucudan gelmesine kalıcı olarak bağımlı kalmayın.

BLE yolunda hangi kodların üretileceği firmware geliştirmesi sırasında netleştirilecek; ön-ek ve
bant yapısı aynı olacak.

---

## 10. Kodun SDK'yı kullanan uygulamaya taşınması

Bu maddenin atlanması, çalışmanın tamamını anlamsız kılar. Kodun sunucudan doğru okunması tek
başına yeterli değildir; **MAC+ uygulamasına ulaşması** gerekir.

### 10.1 Yakalama — en olası hata burada

Sunucu kodu **başarı ve hata yanıtlarının hepsinde** gönderir: 200, 400, 401, 403, 404, 408, 500.
Çoğu HTTP istemcisi 2xx dışındaki yanıtlarda istisna fırlatır ve gövdeyi atar. Bu davranış
korunursa **kodların büyük çoğunluğu kaybolur** — çünkü kodun asıl değerli olduğu durumlar
(üye reddi, kapı açılamadı, servis hatası) hepsi 2xx dışındadır.

Yapılması gereken: HTTP durum kodundan bağımsız olarak yanıt gövdesini ayrıştırın. Gövde
ayrıştırılamazsa veya `code` alanı yoksa `null` ile devam edin; istek hiç tamamlanmadıysa
(timeout, ağ hatası, DNS) sunucudan bir kod gelmemiştir ve `null` doğru değerdir.

### 10.2 Uygulamaya iletme

Aşağıdaki yüzey, HUB ile yapılan önceki yazışmalarda SDK tarafından tarif edilen yapıdır.
**Güncel SDK'daki karşılığını doğrulayıp ona göre uyarlayın**; isimler değişmiş olabilir.

- `onPassFlowStateChanged(state:)` geri bildirimi
- `.completed(result)` durumundaki `PassFlowResult` nesnesi — bugün `result`, `direction`,
  `clubName`, `message` alanlarını taşıyor

`PassFlowResult` nesnesine **`code` alanı eklenmelidir**; `message` alanının hemen yanında ve
onunla aynı yaşam döngüsünde. Bugün `message`'ın ulaştığı her yere `code` da ulaşmalıdır.

### 10.3 Mevcut `result: Int` ile ilişkisi — birbirinin yerine geçmez

SDK'nın kendi akış sonucu (`1 CANCEL`, `2 SUCCESS`, `3 FAIL`, `4 FAIL_PERMISSION`,
`5 FAIL_BLE_DISABLED`, `6 FAIL_LOCATION_TIMEOUT`) **korunur**. Sunucu kodu onun yerini almaz,
yanına eklenir. İkisi farklı soruları yanıtlar:

| `result`                  | Ne demek                              | `code` beklentisi                                                                                                           |
| ------------------------- | ------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `1` CANCEL                | Akış uygulama tarafından iptal edildi | **`null`** — sunucuya gidilmedi                                                                                             |
| `2` SUCCESS               | Geçiş başarılı                        | `A-1001` veya `A-1002`                                                                                                      |
| `3` FAIL                  | Genel başarısızlık                    | **Dolu olması beklenir.** Üye reddi, servis hatası, kapı açılamadı — hepsi buraya düşüyor ve ayrımı yalnızca `code` veriyor |
| `4` FAIL_PERMISSION       | İzin eksik (Bluetooth/konum)          | **`null`** — cihaz tarafı, sunucuya gidilmedi                                                                               |
| `5` FAIL_BLE_DISABLED     | Bluetooth kapalı                      | **`null`** — cihaz tarafı                                                                                                   |
| `6` FAIL_LOCATION_TIMEOUT | Konum doğrulama zaman aşımı           | **`null`** — cihaz tarafı                                                                                                   |

**Bu tablonun pratik sonucu:** MAC+ bugün `result: 3` geldiğinde tek bir statik metin
gösteriyor. Kazanılan asıl yetenek, o tek kovanın `code` ile ayrıştırılabilir hâle gelmesidir —
"günlük limitin doldu", "kart bloke", "kapı açılamadı, tekrar dene" ve "servis geçici olarak
kullanılamıyor" artık birbirinden ayrılabilir.

`code` alanı bu yüzden **nullable** olmalıdır. `null` gelmesi hata değildir; sunucuya hiç
gidilmediği anlamına gelir ve MAC+ bu durumda mevcut statik metinlerini kullanmaya devam eder.

### 10.4 BLE yolu

Geçiş BLE üzerinden tamamlandığında kod sunucudan değil cihazdan gelir. Uygulamaya iletilen alan
**aynı alan olmalıdır**; MAC+ kodun hangi yoldan geldiğini bilmek zorunda kalmamalıdır.

O yolda paket boyutu nedeniyle `message` taşınamayabilir. Bu durumda `code` dolu, `message` boş
gelir — MAC+'ın kendi metin tablosunu kurmasını zorunlu kılan senaryo tam olarak budur.

### 10.5 Tetikleme tipi 3 ve 4 — iki yoldan iki kod gelebilir

QR kodları dört tetikleme tipinden biriyle yapılandırılıyor ve tip, erişim noktası listesinde
`t` alanıyla SDK'ya geliyor:

| Değer | Anlam                         |
| ----- | ----------------------------- |
| 1     | Yalnızca Bluetooth            |
| 2     | Yalnızca uzaktan (sunucu)     |
| 3     | Önce Bluetooth, sonra uzaktan |
| 4     | Önce uzaktan, sonra Bluetooth |

**3 ve 4'te tek bir geçiş denemesi için iki yol da çalışabilir.** Bu durumda SDK ardışık olarak
hem cihazdan hem sunucudan kod alır ve uygulamaya **tek bir kod** iletmesi gerekir.

**Kural — bunu uygulayacak yer SDK'dır.** Cihaz sunucu denemesinden, sunucu da BLE denemesinden
habersizdir; iki yolu yalnızca SDK sıralar, dolayısıyla seçim başka bir katmanda yapılamaz.

1. Bir yol başarıyla sonuçlandıysa (`A-1001` / `A-1002`) o kod geçerlidir, diğer yolun kodu yok
   sayılır.
2. Her iki yol da başarısızsa **son denenen yolun kodu** geçerlidir.

Örnek: cihaz Benefits'e ulaşamayıp `A-5001` döndü, ardından uzaktan yol denendi ve sunucu
Benefits'e ulaşıp `B-2` (günlük limit) aldı. Uygulamaya iletilecek olan `B-2`'dir — kullanıcının
gerçek durumu odur.

### 10.6 Analitik (isteğe bağlı, HUB'a sorulmalı)

`POST /api/v2/analytics` ucunun mevcut şeması `accessTime`, `duration`, `method`, `clubId`,
`qrCodeId`, `direction`, `result`, `os`, `steps` alanlarını kabul ediyor; **sonuç kodu için bir
alan yok**. Kodun analitiğe de yazılması isteniyorsa sunucu şemasının genişletilmesi gerekir —
kendi başınıza alan eklemeyin, HUB ekibine sorun.

---

## 11. Yapılması istenenler

**Sunucu yanıtının okunması**

1. `/access` yanıt modeline `code: String?` alanını ekleyin; `message` mevcut hâliyle kalsın.
2. Yanıt gövdesini **HTTP durum kodundan bağımsız olarak** ayrıştırın (Bölüm 10.1). 2xx dışındaki
   yanıtlarda gövdenin atıldığı bir istemci davranışı varsa düzeltin.
3. `Accept-Language` başlığının gönderildiğini doğrulayın.

**Uygulamaya taşınması**

4. `PassFlowResult`'a (veya güncel karşılığına) nullable `code` alanını ekleyin ve
   `onPassFlowStateChanged` ile uygulamaya iletin. Bugün `message`'ın ulaştığı her yere `code` da
   ulaşmalıdır (Bölüm 10.2).
5. Mevcut `result: Int` değerini **değiştirmeyin**; `code` onun yanına eklenir (Bölüm 10.3).
6. BLE yolunda üretilen kodun **aynı alanla** iletildiğini doğrulayın (Bölüm 10.4).
7. Tetikleme tipi 3 ve 4'te iki yoldan gelen kodlardan hangisinin geçerli olduğunu Bölüm 10.5'teki
   kurala göre seçin ve uygulamaya **tek bir kod** iletin.

**Dayanıklılık**

8. Ön-ek bazlı varsayılan eşlemeyi (Bölüm 3) kurun; bilinen kodlar için özel davranışı bunun
   üzerine ekleyin.
9. Bilinmeyen kod geldiğinde ön-ek varsayılanına düşüp `message`'ı gösterin.
10. Kodu tanımayan eski sürümlerin etkilenmediğini doğrulayın (alan eklemelidir, hiçbir alan
    kaldırılmadı).
11. **Bölüm 1'deki soruyu yanıtlayın:** yayındaki SDK sürümleri `message` alanını 200 ve 408
    durumlarında okuyup iletiyor mu? Cevabı geliştirmeye başlamadan önce HUB'a bildirin.

**Kabul kriteri:** Aşağıdaki dört senaryonun her birinde MAC+'ın eline farklı bir `code`
ulaşmalıdır — başarılı geçiş, MultiSport günlük limit reddi, kapı açılamaması ve bir servis
kesintisi. Dördü de bugün `result: 3` veya `result: 2` altında ayrışmadan geliyor.

## 12. Varsayım yapılmaması gerekenler

- **Kod listesinin kapalı olduğunu varsaymayın.** `B-` ve `C-` altında listede olmayan kodlar
  gelebilir; bu tasarımın amacıdır.
- **`message`'ın her zaman dolu olduğunu varsaymayın.** Sözleşmede opsiyoneldir; boş gelirse
  ön-ek varsayılanınızın kendi metnini gösterin.
- **HTTP durum kodundan kod türetmeyin.** Aynı statüde farklı kodlar gelebilir (örneğin 401'de
  `A-2001`, `A-2002`, `B-*`, `C-*`; 500'de `A-5001`, `A-6001`, `A-9999`).
- **Koddan HTTP statüsü türetmeyin.** `A-4001` hem 400 hem 403 ile gelebilir.
- **Handshake ve imza doğrulama için geliştirme yapmayın** (Bölüm 8).
- **`code`'un her zaman dolu olduğunu varsaymayın.** Sunucuya hiç gidilmeyen akışlarda (iptal,
  izin eksikliği, Bluetooth kapalı, konum zaman aşımı) `null` gelir ve bu doğru davranıştır.
- **`code`'un mevcut `result: Int` değerinin yerine geçtiğini varsaymayın.** İkisi birlikte
  taşınır.

---

## Soru çıkarsa

Sunucu tarafındaki uygulama tamamlanmış ve doğrulanmıştır. Kod listesi, HTTP eşlemeleri veya
mesaj metinleriyle ilgili belirsizlikte tahmin yürütmek yerine HUB ekibine sorun.

**Bizden beklenen tek geri bildirim** Bölüm 1'deki sorudur: yayındaki SDK sürümlerinin `message`
alanını başarı ve zaman aşımı durumlarında işleyip işlemediği. Diğer her şey bu dokümanda
tanımlıdır.
