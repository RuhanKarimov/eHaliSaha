# YDG Final – Jenkins CI/CD Checklist (eHalisaha)

Bu repo, dersteki **6 aşamayı** (Checkout → Build → Unit → IT → Docker → Selenium senaryoları) karşılayacak şekilde hazırlanmıştır.

## Pipeline Aşamaları (puan karşılığı)

1) **Github'dan kodlar çekilecek (5p)**
- Jenkinsfile: `stage('1-Checkout (GitHub)')`

2) **Kodlar build edilecek (5p)**
- `stage('2-Build')`: `./mvnw ... package` (Unit + IT kapalı)

3) **Birim Testleri çalıştırılacak ve raporlanacak (15p)**
- `stage('3-Unit Tests (Surefire)')`: `./mvnw ... test`
- Rapor: `target/surefire-reports/*.xml` (Jenkins `junit`)

4) **Entegrasyon testleri çalıştırılacak ve raporlanacak (15p)**
- `stage('4-Integration Tests (Failsafe)')`: `./mvnw ... verify`
- Rapor: `target/failsafe-reports/*.xml` (Jenkins `junit`)

5) **Sistem docker container'lar üzerinde çalıştırılacak (5p)**
- `stage('5-Run System on Docker Containers')`: `docker compose -f docker-compose.ci.yml up -d --build`

6) **Çalışır durumdaki sistem üzerinden Selenium ile test senaryoları (55p + bonus)**
- Her senaryo Jenkins’te ayrı stage olarak çalışır.
- Çıktı: `e2e-reports/*.xml` (Jenkins `junit`)

## Selenium Test Senaryoları (toplam 10)

> Duyurudaki “en az 3, en fazla 10” şartını karşılar.  
> Bu repo, tam puan için **10 senaryo** içerir.

1. Owner login → owner panel açılır
2. Owner facility oluşturur
3. Owner slot preset uygular + slotları kaydeder + pitch ekler + 60dk fiyat girer
4. Member login → member panel açılır uygular + slotları kaydeder + pitch ekler + 60dk fiyat girer
5. Member üyelik isteği gönderir
6. Owner üyelik isteğini onaylar
7. Member rezervasyon yapar (oyuncu + ödeme + shuttle)
8. Aynı slotu tekrar rezerve etmeye çalışınca conflict engellenir (UI: DOLU, API: 409)
9. Owner rezervasyon ledger sayfasında member rezervasyonunu görür
10. Member aktif üyelik varken tekrar üyelik isteği gönderemez

## Lokal çalıştırma (CI ile aynı)

```bash
docker compose -f docker-compose.ci.yml up -d --build

# Tek tek senaryo çalıştırma örnekleri
docker compose -f docker-compose.ci.yml run --rm e2e pytest -q tests/test_01_owner_login.py   --junitxml=/reports/selenium_01_owner_login.xml

docker compose -f docker-compose.ci.yml run --rm e2e pytest -q tests/test_09_owner_reservations_list.py   --junitxml=/reports/selenium_09_owner_ledger.xml

docker compose -f docker-compose.ci.yml down -v
```

## Notlar
- Jenkins agent tarafında **Docker** kurulu olmalı (stage 5-6 için).
- Jenkins server genelde 8080 kullandığı için CI compose dosyasında **host port publish yok** (çatışma yaşamamak için).
- Demo kullanıcılar:
  - OWNER: `owner1 / owner123`
  - MEMBER: `member1 / member123`


## Selenium E2E (Java)
- /e2e-java klasörü: JUnit5 + Selenium RemoteWebDriver
- Jenkins Stage 6.*: docker compose run e2e-java ./run_scenario.sh ...


## Not: E2E Testleri src/test/java içinde (Java)
- E2E sınıf isimleri `*E2E*.java` olmalı (örn. `Scenario01OwnerLoginE2E`).
- `mvn -P e2e -Dtest=<SinifAdi> test` ile sadece tek E2E senaryosu koşar.
- Jenkins, `docker-compose.ci.yml` ile app (18080) ve selenium (14444) portlarını host'a açar.
