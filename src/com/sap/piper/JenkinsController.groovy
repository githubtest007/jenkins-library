package com.sap.piper

class JenkinsController implements Serializable {
    def script
    String jenkinsUrl
    def timeout

    JenkinsController(script, String jenkinsUrl = "http://localhost:8080", timeout = 3600) {
        this.script = script
        this.jenkinsUrl = jenkinsUrl
        this.timeout = timeout
    }

    def waitForJenkinsStarted() {
        def timeout = 120
        def timePerLoop = 5

        for (int i = 0; i < timeout; i += timePerLoop) {
            script.sleep timePerLoop
            try {
                if (retrieveJenkinsStatus() == 'NORMAL') {
                    return true
                }
            } catch (Exception e) {
                script.echo "Could not retrieve status for Jenkins. Retrying..."
                script.echo e.getMessage()
                continue
            }
            return false
        }
        script.error("Timeout: Build of waiting for Jenkins status}")
    }

    private retrieveJenkinsStatus() {
        def apiUrl = "${jenkinsUrl}/api/json"
        script.echo "Checking Jenkins Status"
        def response = new URL(apiUrl).getText()
        def result = script.readJSON text: response
        return result.mode
    }

    //Trigger scanning of the multi branch builds
    def buildJob(String jobName) {
        script.sh "curl -s -X POST ${jenkinsUrl}/job/${jobName}/build"
    }

    def waitForSuccess(String jobName, String branch) {
        if (this.waitForJobStatus(branch, 'SUCCESS')) {
            this.printConsoleText(branch)
            script.echo "Build was successful"
        } else {
            this.printConsoleText(branch)
            script.error("Build of ${jobName} ${branch} was not successfull")
        }
    }

    def getBuildUrl(String jobName, String branch) {
        return "${jenkinsUrl}/job/${jobName}/job/${URLEncoder.encode(branch, "UTF-8")}/lastBuild/"
    }

    def waitForJobStatus(String jobName, String branch, String status) {
        def buildUrl = getBuildUrl(branch)
        def timePerLoop = 10

        for (int i = 0; i < timeout; i += timePerLoop) {
            script.sleep timePerLoop
            try {
                script.echo "Checking Build Status of ${jobName} ${branch}"
                def buildInformation = retrieveBuildInformation(branch)

                if (buildInformation.building) {
                    script.echo "Build is still in progress"
                    continue
                }
                if (buildInformation.result == status) {
                    return true
                }
            } catch (Exception e) {
                script.echo "Could not retrieve status for ${buildUrl}. Retrying..."
                script.echo e.getMessage()
                continue
            }
            return false
        }
        script.error("Timeout: Build of waiting for ${jobName} ${branch}")
    }

    def getConsoleText(String branch) {
        def consoleUrl = this.getBuildUrl(branch) + "/consoleText"
        return new URL(consoleUrl).text
    }

    def printConsoleText(String branch) {
        String consoleOutput = getConsoleText(branch)

        script.echo '***********************************************'
        script.echo '** Begin Output of Example Application Build **'
        script.echo '***********************************************'

        script.echo consoleOutput

        script.echo '*********************************************'
        script.echo '** End Output of Example Application Build **'
        script.echo '*********************************************'
    }

    def retrieveBuildInformation(String jobName, String branch) {
        def buildUrl = getBuildUrl(branch)
        def url = "${buildUrl}/api/json"
        script.echo "Checking Build Status of ${jobName} ${branch}"
        script.echo "${jenkinsUrl}/job/${jobName}/job/${URLEncoder.encode(branch, "UTF-8")}/"
        def response = script.fetchUrl(url)
        def result = script.readJSON text: response
        return result
    }

}
