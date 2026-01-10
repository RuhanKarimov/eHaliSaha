// ---------------- helpers (pipeline dışı) ----------------

def chromeArgsCommon() {
    return "--disable-features=HttpsOnlyMode,UpgradeInsecureRequests,HttpsFirstMode,HttpsFirstModeV2,HttpsUpgrades,AutomaticHttpsUpgrades;--ignore-certificate-errors;--allow-insecure-localhost"
}

def e2eEnv(String chromeArgs) {
    // ✅ Browser container’da -> app servisine docker network’ten eriş
  return [
    "BASE_URL=http://app:8080",
    // ✅ Maven host’ta koşuyor -> selenium host portundan eriş
    "SELENIUM_URL=http://localhost:14444/wd/hub",
    "CHROME_ARGS=${chromeArgs}"
  ]
}

def runE2E(String testClass) {
    def chromeArgs = chromeArgsCommon()
  def envs = e2eEnv(chromeArgs)

  withEnv(envs) {
        if (isUnix()) {
            sh "./mvnw ${env.MVN_ARGS} -P e2e -Dtest=${testClass} test"
    } else {
            bat ".\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=${testClass} test"
    }
  }
}

// ---------------- pipeline ----------------

pipeline {
    agent any

  options {
        timestamps()
  skipDefaultCheckout(true)
  disableConcurrentBuilds()   // aynı anda 2 build -> birbirinin dockerını silmesin
}


  environment {
        COMPOSE_FILE = 'docker-compose.ci.yml'
  COMPOSE_PROJECT_NAME = 'ehalisaha-ydg'   // <-- ekle (compose prefix sabit olur)
  MVN_ARGS = '-U -B -Dfile.encoding=UTF-8'
  JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
}


  stages {

        stage('Checkout') {
            steps {
                checkout(
          changelog: false,
          poll: false,
          scm: [
            $class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[url: 'https://github.com/RuhanKarimov/eHaliSaha.git']]
          ]
        )
      }
    }

    stage('2-Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipTests package"
          } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipTests package"
          }
        }
      }
    }

    stage('3-Unit Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipUnitTests=false -DskipITs=true test"
          } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipUnitTests=false -DskipITs=true test"
          }
        }
      }
      post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('4-Integration Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipUnitTests=true -DskipITs=false verify"
          } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipUnitTests=true -DskipITs=false verify"
          }
        }
      }
      post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('5-Run System on Docker') {
            steps {
                script {
                    if (isUnix()) {
                        sh "docker compose -f ${env.COMPOSE_FILE} down -v --remove-orphans || true"
                        sh "docker compose -f ${env.COMPOSE_FILE} up -d --build"
                        sh "docker compose -f ${env.COMPOSE_FILE} ps"

                        sh "docker compose -f ${env.COMPOSE_FILE} up -d --build"
                        sh "docker compose -f ${env.COMPOSE_FILE} ps"
                    } else {
                        bat 'docker compose -f %COMPOSE_FILE% down -v --remove-orphans || ver>nul'
                        bat 'docker compose -f %COMPOSE_FILE% up -d --build'
                        bat 'docker compose -f %COMPOSE_FILE% ps'


                        bat 'docker compose -f %COMPOSE_FILE% up -d --build'
                        bat 'docker compose -f %COMPOSE_FILE% ps'

        // ✅ CMD quoting çilesi yok: PowerShell'i dosyadan çalıştır
        writeFile file: 'wait-ci.ps1', encoding: 'UTF-8', text: '''
$ErrorActionPreference = "SilentlyContinue"
$compose = $env:COMPOSE_FILE

function Wait-Url([string]$url, [int]$tries, [int]$sleepSec) {
  for($i=0; $i -lt $tries; $i++){
    try {
      $r = Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2
      if($r -and $r.StatusCode -eq 200){
        Write-Host ("OK: " + $url + " => " + $r.StatusCode)
        return $true
      }
    } catch {}
    Start-Sleep -Seconds $sleepSec
  }
  return $false
}

# 1) Selenium Grid ready (host -> 14444)
if(-not (Wait-Url "http://localhost:14444/status" 60 2)){
  Write-Host "FAIL: Grid not ready"
  & docker compose -f $compose logs --no-color selenium | Out-Host
  exit 1
}

# 2) App ready (host port mapping)
# compose ps çıktında 18080->8080 görünüyor, önce 18080 dene
$ok = Wait-Url "http://localhost:18080/api/public/ping" 120 2

# bazı makinelerde farklı mapping varsa diye opsiyonel fallback
if(-not $ok){
  $ok = Wait-Url "http://localhost:8080/api/public/ping" 30 2
}

if(-not $ok){
  Write-Host "FAIL: App not ready on host"
  & docker compose -f $compose ps | Out-Host
  & docker compose -f $compose logs --no-color app | Out-Host
  exit 1
}

Write-Host "READY: Selenium + App"
exit 0
'''

        bat 'powershell -NoProfile -ExecutionPolicy Bypass -File wait-ci.ps1'
      }
    }
  }
}


    stage('5.5-Smoke: Selenium -> App network check') {
            steps {
                script {
                    if (isUnix()) {
                        sh "docker compose -f ${env.COMPOSE_FILE} ps"
            // Linux agent varsa burada da smoke eklenebilir, şimdilik dokunmuyorum.
          } else {
                        bat "docker compose -f %COMPOSE_FILE% ps"

            writeFile file: 'smoke.ps1', encoding: 'UTF-8', text: '''
$ErrorActionPreference = "SilentlyContinue"
$compose = $env:COMPOSE_FILE

function ExecSelenium([string]$innerCmd) {
  & docker compose -f $compose exec -T selenium sh -lc $innerCmd 2>&1
}

$ok = $false

for($i=0; $i -lt 90; $i++){
  Write-Host ("[try " + $i + "] curl -i http://app:8080/api/public/ping")
  $out = ExecSelenium "curl -sS -i http://app:8080/api/public/ping || true"
  if($out){ $out | ForEach-Object { Write-Host $_ } }

  if($out -match "HTTP/[^ ]+ 200"){
    Write-Host "OK: Selenium can reach app:8080"
    $ok = $true
    break
  }

  Start-Sleep -Seconds 2
}

if(-not $ok){
  Write-Host "FAIL: Selenium cannot reach app:8080"
  Write-Host "---- selenium logs ----"
  & docker compose -f $compose logs --no-color selenium | Out-Host
  Write-Host "---- app logs ----"
  & docker compose -f $compose logs --no-color app | Out-Host
  exit 1
}

Write-Host "[ui check] http://app:8080/ui/login.html?role=OWNER"
ExecSelenium "curl -sS -I 'http://app:8080/ui/login.html?role=OWNER' || true" | ForEach-Object { Write-Host $_ }

exit 0
'''
            bat 'powershell -NoProfile -ExecutionPolicy Bypass -File smoke.ps1'
          }
        }
      }
      post {
                always {
                    script {
                        if (!isUnix()) {
                            bat 'docker compose -f %COMPOSE_FILE% logs --no-color app || ver>nul'
              bat 'docker compose -f %COMPOSE_FILE% logs --no-color selenium || ver>nul'
              bat 'docker compose -f %COMPOSE_FILE% ps'
            }
          }
        }
      }
    }

    // ---------------- E2E stages (6.1 - 6.10) ----------------
    stage('6.1-E2E Scenario 1: Owner Login') {
            steps { script { runE2E("Scenario01OwnerLoginTestE2E") } }
    }

    stage('6.2-E2E Scenario 2: Owner Creates Facility') {
            steps { script { runE2E("Scenario02OwnerCreateFacilityTestE2E") } }
    }

    stage('6.3-E2E Scenario 3: Owner Setup Slots Pitch Pricing') {
            steps { script { runE2E("Scenario03OwnerSetupSlotsPitchPricingTestE2E") } }
    }

    stage('6.4-E2E Scenario 4: Member Login') {
            steps { script { runE2E("Scenario04MemberLoginTestE2E") } }
    }

    stage('6.5-E2E Scenario 5: Member Sends Membership Request') {
            steps { script { runE2E("Scenario05MemberSendsMembershipRequestTestE2E") } }
    }

    stage('6.6-E2E Scenario 6: Owner Approves Membership') {
            steps { script { runE2E("Scenario06OwnerApprovesMembershipRequestTestE2E") } }
    }

    stage('6.7-E2E Scenario 7: Member Makes Reservation') {
            steps { script { runE2E("Scenario07MemberMakesReservationTestE2E") } }
    }

    stage('6.8-E2E Scenario 8: Double Booking Should Fail') {
            steps { script { runE2E("Scenario08DoubleBookingShouldFailTestE2E") } }
    }

    stage('6.9-E2E Scenario 9: Owner Sees Reservation List') {
            steps { script { runE2E("Scenario09OwnerSeesReservationInListTestE2E") } }
    }

    stage('6.10-E2E Scenario 10: Membership Already Active Should Fail') {
            steps { script { runE2E("Scenario10MembershipAlreadyActiveShouldFailTestE2E") } }
    }
  }

  post {
        always {
            archiveArtifacts artifacts: 'e2e-reports/*', allowEmptyArchive: true

    // Docker AYAKTA KALSIN diye sadece durum/log basıyoruz
    script {
                if (isUnix()) {
                    sh "docker compose -f ${env.COMPOSE_FILE} ps || true"
        sh "docker compose -f ${env.COMPOSE_FILE} logs --no-color --tail=80 app || true"
        sh "docker compose -f ${env.COMPOSE_FILE} logs --no-color --tail=80 selenium || true"
      } else {
                    bat "docker compose -f %COMPOSE_FILE% ps"
        bat "docker compose -f %COMPOSE_FILE% logs --no-color --tail=80 app || ver>nul"
        bat "docker compose -f %COMPOSE_FILE% logs --no-color --tail=80 selenium || ver>nul"
      }
    }
  }
}

}
