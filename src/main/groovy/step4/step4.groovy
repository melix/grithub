package step4

import org.codehaus.groovy.runtime.MetaClassHelper
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

class Wrapper {
    def delegate

    def methodMissing(String name, args) {
        try {
            delegate.invokeMethod(name, args)
        } catch (MissingMethodException e) {
            if (args.length == 2 && args[-1] instanceof Closure) {
                def metaMethod = delegate.metaClass.getMetaMethod("get${MetaClassHelper.capitalize(name)}", args[0])
                if (metaMethod) {
                    // ex: organization('foo') { ... } ---> getOrganization('foo').with { ... }
                    def result = WrapperFactory.wrap(metaMethod.invoke(delegate, args[0]))
                    result.with(args[1])
                }
            } else if (args.length > 0) {
                def metaMethod = delegate.metaClass.getMetaMethod("get${MetaClassHelper.capitalize(name)}", args)
                if (metaMethod) {
                    // ex: getPullRequests('foo') ---> pullRequests('foo')
                    def result = metaMethod.invoke(delegate, args)
                    if (result instanceof List) {
                        result = result.collect { WrapperFactory.wrap(it) }
                    }
                    result
                }
            } else {
                throw e
            }
        }
    }

    def propertyMissing(String name) {
        delegate."$name"
    }
}

class GroovyGitHub extends Wrapper {

    public static void session(Closure c) {
        def gh = new GroovyGitHub(delegate: GitHub.connect())
        def beforeLimit = gh.rateLimit
        gh.with(c)
        def afterLimit = gh.rateLimit
        println "Consumed ${beforeLimit.remaining - afterLimit.remaining} API calls. Remaining: ${afterLimit.remaining}"
    }

}

class GHRepositoryWrapper extends Wrapper {
    def pullRequests(String str) {
        getPullRequests(GHIssueState.valueOf(str))
    }

    def getPullRequests() {
        [:].withDefault { String key ->
            pullRequests(key)
        }
    }
}
class WrapperFactory {
    private final static MAPPING
    static {
        def map = [:].withDefault { Wrapper }
        map[GitHub] = GroovyGitHub
        map[GHRepository] = GHRepositoryWrapper
        MAPPING = map.asImmutable()
    }

    public static Wrapper wrap(o) {
        MAPPING.get(o.class).newInstance(delegate: o)
    }
}

GroovyGitHub.session {
    organization('groovy') {
        repository('groovy-core') {
            pullRequests.OPEN.each {
                println "Pull request by ${it.user.name ?: it.user.login}: ${it.title}"
            }
        }
    }
}
