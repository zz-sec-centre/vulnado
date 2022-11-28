#!groovy

pipeline {
    // agent { label 'maven' }
    agent { label 'docker-agent' }

    environment {
        MODULE = 'vulnado'
        DOCKER_FILE = 'Dockerfile.vulnado'
        DOCKERHUB_ACNT = 'prakashsethuraman'
        APP_URL = "www.qa.cbc.beescloud.com"
        PIPELINE_CHECK = "https://${APP_URL}/api/external/webhook/pipeline-compliance-check"
        COMPLIANCE_CHECK = "https://${APP_URL}/api/external/webhook/compliance-check"
    }

    options {
        // disableConcurrentBuilds()
        skipDefaultCheckout()
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        
        stage('Pipeline Compatibility Check') {
            steps {
              script {
                echo "This step will check whether all expected steps/checks are configured in pipeline or not"
                  
                echo "SonarQube and Compliance Check is configured.. Good to go for next step"
              }
            }
          }

        stage('Start') {
            steps {
              cleanWs()
              script {
                currentBuild.description = "${env.GIT_BRANCH} ${env.GIT_COMMIT}"
              }
            }
          }


        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Pre-build') {
            steps {
              script {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "dockerhub_creds", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                //   GIT_SHORT_HASH = env.GIT_COMMIT.take(7)
                  REQST_TIME_STAMP = sh (script: "date -u +'%Y-%m-%dT%H:%M:%SZ'", returnStdout: true).trim() 
                  REQST_ID = sh (script: "date +%s", returnStdout: true).trim()                   
                  TARGET_DOCKERHUB = sh (script: "echo ${DOCKERHUB_ACNT}/${MODULE}:latest", returnStdout: true).trim()     
                  sh '''
                  echo "Login into hub.docker.com"
                  docker login --username $USERNAME --password $PASSWORD
                  '''
                }
              }
            }
          }


        stage('Docker-Build') {
            steps {
                script {
                    sh '''
                        docker build -t ${MODULE}  -f $DOCKER_FILE .
                        ID=$(docker create ${MODULE})
                        echo ${ID}
                        docker cp ${ID}:/src/target ./
                        docker rm ${ID}
                        pwd
                        ls target
                        '''
                }
            }
        }


        stage('SonarQube') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withMaven(
                        options: [junitPublisher(disabled: true, healthScaleFactor: 1.0)],
                        publisherStrategy: 'EXPLICIT') {
                            sh 'mvn sonar:sonar -Dsonar.projectKey=${MODULE}'
                    }
                }
            }
        }


        stage('Compliance Check') {
            steps{
                script {
                final def (String response, String code) =
                    sh(script: """curl -X POST -d '{"requestSource": "CBCI", "requestId" : "${REQST_ID}", "requestTimestamp" : "${REQST_TIME_STAMP}", "details" : {"project" : "<<Project Name>>", "release" : "<<Release Name>>", "pipeline" : "<<Pipeline Name>>" } }' -s -w "\\n%{response_code}" ${PIPELINE_CHECK}""", returnStdout: true)                
                        .trim()
                        .tokenize('\n')

                if (code == null) {
                    code = Integer.parseInt(response)
                }

                if (code == "200") {
                    def json = readJSON text: response
                    def status = json.complianceCheckStatus
                    echo "status = $status"
                    if (status != "APPROVED") {
                        error "Pipeline compliance check - failed"
                    }
                } else {
                    echo "Failed to check compliance with CBC"
                    error "Failed to check compliance with CBC"
                }
                }
            }
        }    


        stage('DockerHub-Push') {
            steps {
            script {
                sh "docker tag $MODULE $TARGET_DOCKERHUB"
                sh "docker push -q $TARGET_DOCKERHUB"
                }
            }
        }
        
        stage('Trigger CloudBees CD Pipeline') {
            steps {
                cloudBeesFlowTriggerRelease configuration: 'CloudBees CD',
                                            parameters: '{"release":{"releaseName":"CBC Demo Release","pipelineName":"pipeline_CBC Demo Release","parameters":[]}}',
                                            projectName: 'CBC Demo',
                                            releaseName: 'CBC Demo Release'
            }
        }
    }
}
