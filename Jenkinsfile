
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)

    }

    environment {
        COMPOSE_FILE = 'docker-compose.ci.yml'
        MVN_ARGS = '-U -B'
        BASE_URL = 'http://app:8080'
        SELENIUM_URL = 'http://localhost:14444/wd/hub'
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
                // Up
    bat 'docker compose -f %COMPOSE_FILE% up -d --build'

    // Debug: servisleri göster
    bat 'docker compose -f %COMPOSE_FILE% ps'

    // 1) Selenium Grid ready (host -> localhost:14444/status)
    bat '''
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='SilentlyContinue'; $ok=$false; for($i=0;$i -lt 60;$i++){ try { $r=Invoke-WebRequest 'http://localhost:14444/status' -UseBasicParsing -TimeoutSec 2; if($r -and $r.StatusCode -eq 200){ Write-Host ('Grid ready: ' + $r.StatusCode); $ok=$true; break } } catch {} Start-Sleep -Seconds 2 }; if(-not $ok){ Write-Host 'Grid not ready'; cmd /c \"docker compose -f %COMPOSE_FILE% logs --no-color selenium\"; exit 1 }"
'''

    // 2) App ready (host -> localhost:18080/api/public/ping)
    //    - burada 18080 host port mapping'i; Jenkins host üzerinden garanti check
    bat '''
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='SilentlyContinue'; $ok=$false; for($i=0;$i -lt 120;$i++){ try { $r=Invoke-WebRequest 'http://localhost:18080/api/public/ping' -UseBasicParsing -TimeoutSec 2; if($r -and $r.StatusCode -eq 200){ Write-Host ('App ready on host:18080: ' + $r.StatusCode); $ok=$true; break } } catch {} Start-Sleep -Seconds 2 }; if(-not $ok){ Write-Host 'App not ready on host:18080'; cmd /c \"docker compose -f %COMPOSE_FILE% logs --no-color app\"; cmd /c \"docker compose -f %COMPOSE_FILE% ps\"; exit 1 }"
'''
  }
}






        stage('5.5-Smoke: Selenium -> App network check') {
            steps {
                // Görsel debug
    bat 'docker compose -f %COMPOSE_FILE% ps'

    // 1) Selenium container içinde curl var mı? Yoksa kur. Sonra app:8080 ping dene.
    //    90 deneme * 2sn = 180sn max
    bat '''
docker compose -f %COMPOSE_FILE% exec -T selenium sh -lc "set -e;
  (command -v curl >/dev/null 2>&1) || (apt-get update >/dev/null 2>&1 && apt-get install -y curl >/dev/null 2>&1) || (apk add --no-cache curl >/dev/null 2>&1) || true;
  i=0;
  while [ $i -lt 90 ]; do
    echo \"[try $i] curl -i http://app:8080/api/public/ping\";
    out=$(curl -sS -i http://app:8080/api/public/ping 2>&1 || true);
    echo \"$out\";
    echo \"$out\" | grep -E \"HTTP/[^ ]+ 200\" >/dev/null 2>&1 && echo \"OK: Selenium can reach app:8080\" && exit 0;
    i=$((i+1));
    sleep 2;
  done;
  echo \"FAIL: Selenium cannot reach app:8080\";
  exit 1"
'''

    // 2) (opsiyonel ama faydalı) app UI sayfası dönüyor mu?
    bat '''
docker compose -f %COMPOSE_FILE% exec -T selenium sh -lc "set -e;
  (command -v curl >/dev/null 2>&1) || true;
  echo '[ui check] http://app:8080/ui/login.html?role=OWNER';
  curl -sS -I http://app:8080/ui/login.html?role=OWNER || true"
'''
  }

  post {
                always {
                    // başarısız olursa teşhis için logları dök (job çıktısına)
      bat 'docker compose -f %COMPOSE_FILE% logs --no-color app || ver>nul'
      bat 'docker compose -f %COMPOSE_FILE% logs --no-color selenium || ver>nul'
    }
  }
}












        stage('6.1-E2E (Selenium) Scenario 1: Owner Login') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario01OwnerLoginTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario01OwnerLoginTestE2E test"
                    }
                }
            }
        }

        stage('6.2-E2E Scenario 2: Owner Creates Facility') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario02OwnerCreateFacilityTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario02OwnerCreateFacilityTestE2E test"
                    }
                }
            }
        }

        stage('6.3-E2E Scenario 3: Owner Setup Slots Pitch Pricing') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario03OwnerSetupTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario03OwnerSetupTestE2E test"
                    }
                }
            }
        }

        stage('6.4-E2E Scenario 4: Member Login') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario04MemberLoginTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario04MemberLoginTestE2E test"
                    }
                }
            }
        }

        stage('6.5-E2E Scenario 5: Member Sends Membership Request') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario05MemberSendMembershipRequestTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario05MemberSendMembershipRequestTestE2E test"
                    }
                }
            }
        }

        stage('6.6-E2E Scenario 6: Owner Approves Membership') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario06OwnerApproveMembershipRequestTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario06OwnerApproveMembershipRequestTestE2E test"
                    }
                }
            }
        }

        stage('6.7-E2E Scenario 7: Member Makes Reservation') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario07MemberMakesReservationTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario07MemberMakesReservationTestE2E test"
                    }
                }
            }
        }

        stage('6.8-E2E Scenario 8: Double Booking Should Fail') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario08DoubleBookingShouldFailTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario08DoubleBookingShouldFailTestE2E test"
                    }
                }
            }
        }

        stage('6.9-E2E Scenario 9: Owner Sees Reservation List') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario09OwnerSeesReservationInListTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario09OwnerSeesReservationInListTestE2E test"
                    }
                }
            }
        }

        stage('6.10-E2E Scenario 10: Membership Already Active Should Fail') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario10MembershipAlreadyActiveShouldFailTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario10MembershipAlreadyActiveShouldFailTestE2E test"
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'e2e-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true

            script {
                if (isUnix()) {
                    sh "docker compose -f ${env.COMPOSE_FILE} down -v || true"
                } else {
                    bat "docker compose -f %COMPOSE_FILE% down -v"
                }
            }
        }
    }
}
