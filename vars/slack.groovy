#!/usr/bin/env groovy

/* Libraries for notifying chatrooms. */

import org.jenkinsci.plugins.workflow.support.steps.build.*


def notifyChat(RunWrapper theBuild, message = null) {
    /* Notifies chatroom(s) of the status of a build.

    Usage: sfChat.notifyChat(currentBuild)

    The following environment variables must be set in your Jenkinsfile:

        CHAT_ROOM - this can be a single room or a comma separated list of rooms

        For example:
            environment {
                CHAT_ROOM = 'RoomA, The Room'
            }
    * @param message Optional text to display in slack otherwise a default msg is used.
    */
    if (theBuild.result == "SUCCESS" || theBuild.result == null ) {
        notifyPass(theBuild, message)
    } else if (theBuild.result == "UNSTABLE") {
        notifyUnstable(theBuild, message)
    } else {
        notifyFail(theBuild, message)
    }
}

def sendSlackNotification(color, message) {
    /**
     * Send jenkins notification to slack channel.
     *
     * @param color 'good, warning, danger, or any hex color code (eg. #439FE0)'
     * @param message Text to display in slack
     */
    def channels = "${env.CHAT_ROOM}".tokenize(' ,;')
    channels.each { channel_name ->
        slackSend (channel:"${channel_name}", color:"${color}", message:"${message}")
    }
}

def notifyPass(RunWrapper theBuild, message = null) {
    if (message == null) {
        message =  "${env.JOB_NAME} passed with these changes " + getChangeString(theBuild) + " <${env.BUILD_URL}|(View)>"
    }

    sendSlackNotification ("good", "${message}")
}

def notifyFail(RunWrapper theBuild, message = null) {
    if (message == null) {
        message =  "${env.JOB_NAME} failed with these changes" + getChangeString(theBuild) + " <${env.BUILD_URL}|(View)>"
    }

    sendSlackNotification ("danger", "${message}")
}

def notifyUnstable(RunWrapper theBuild, message = null) {
    if (message == null) {
        message =  "${env.JOB_NAME} is unstable with these changes" + getChangeString(theBuild) + " <${env.BUILD_URL}|(View)>"
    }

    sendSlackNotification ("warning", "${message}")
}

@NonCPS
def getChangeString(RunWrapper theBuild) {
    MAX_MSG_LEN = 100
    def changeString = ""

    def changeLogSets = theBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            def paths = entry.getAffectedPaths();
            // Filter out commits coming from jenkins-libraries itself
            if (paths.any{ it.startsWith('vars/') && it.endsWith('.groovy')}) {
                continue
            }
            truncated_msg = entry.msg.take(MAX_MSG_LEN)
            if (!changeString.contains("${truncated_msg}")) {
                changeString += " - ${truncated_msg} [${entry.author}]\n"
            }
        }
    }

    if (!changeString) {
        changeString = " - No new changes"
    }
    return changeString
}
