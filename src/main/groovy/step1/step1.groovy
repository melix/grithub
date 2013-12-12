package step1

import org.kohsuke.github.GitHub

def github = GitHub.connect()
def repo = github.getOrganization('groovy').getRepository('groovy-core')
def prs = repo.getPullRequests 'OPEN'
prs.each {
    println "Pull request by ${it.user.name?:it.user.login}: ${it.title}"
}
