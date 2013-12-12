package step2

import org.codehaus.groovy.runtime.MetaClassHelper
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub

GitHub.metaClass.static.session = {
    def gh = GitHub.connect()
    def beforeLimit = gh.rateLimit
    gh.with(it)
    def afterLimit = gh.rateLimit
    println "Consumed ${beforeLimit.remaining-afterLimit.remaining} API calls over ${afterLimit.limit}"
}

GitHub.metaClass.invokeMethod = { String name, args ->
    println "Calling '$name' on '$delegate'"
    def metaMethod = GitHub.metaClass.getMetaMethod(name, args)
    if (metaMethod) {
        return metaMethod.invoke(delegate, args)
    }
    if (args.length==2 && args[1] instanceof Closure) {
        metaMethod = GitHub.metaClass.getMetaMethod("get${MetaClassHelper.capitalize(name)}", args[0])
        if (metaMethod) {
            def val = metaMethod.invoke(delegate, args[0])
            return val.with(args[1])
        }
    }
    throw new MissingMethodException(name, delegate.class, args)
}

GitHub.session {
    delegate.organization('groovy') {
        def repo = getRepository('groovy-core')
        def prs = repo.getPullRequests GHIssueState.OPEN
        prs.each {
            println "Pull request by ${it.user.name?:it.user.login}: ${it.title}"
        }
    }
}
