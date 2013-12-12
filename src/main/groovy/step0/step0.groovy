package step0

import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub

def github = GitHub.connect()
def repo = github.getOrganization('groovy').getRepository('groovy-core')
def prs = repo.getPullRequests(GHIssueState.OPEN)
prs.each {
    println "Pull request by ${it.getUser().getName()?:it.getUser().getLogin()}: ${it.getTitle()}"
}
