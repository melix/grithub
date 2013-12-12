package step3

import org.codehaus.groovy.runtime.MetaClassHelper
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub

class Wrapper {
    def delegate

    def methodMissing(String name, args) {
        try {
            delegate.invokeMethod(name,args)
        } catch (MissingMethodException e) {
            if (args.length==2 && args[1] instanceof Closure) {
                def metaMethod = delegate.metaClass.getMetaMethod("get${MetaClassHelper.capitalize(name)}", args[0])
                if (metaMethod) {
                    // ex: organization('foo') { ... } ---> getOrganization('foo').with { ... }
                    def result = WrapperFactory.wrap(metaMethod.invoke(delegate, args[0]))
                    result.with(args[1])
                }
            }
        }
    }

    def propertyMissing(String name) {
        delegate."$name"
    }
}

class GroovyGitHub extends Wrapper {

    public static void session(Closure c) {
        def gh = new GroovyGitHub(delegate:GitHub.connect())
        def beforeLimit = gh.rateLimit
        gh.with(c)
        def afterLimit = gh.rateLimit
        println "Consumed ${beforeLimit.remaining-afterLimit.remaining} API calls. Remaining: ${afterLimit.remaining}"
    }

}

class WrapperFactory {
    public static Wrapper wrap(o) {
        switch (o.class) {
            case GitHub:
                new GroovyGitHub(delegate:o)
                break
            default:
                new Wrapper(delegate:o)
                break
        }
    }
}

GroovyGitHub.session {
    organization('groovy') {
        repository('groovy-core') {
            def prs = getPullRequests GHIssueState.OPEN
            prs.each {
                println "Pull request by ${it.user.name?:it.user.login}: ${it.title}"
            }
        }
    }
}
