package blocks

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
//import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin
import org.springframework.context.support.ReloadableResourceBundleMessageSource.PropertiesHolder

//import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin
class ExposedKeysMessageSource extends PluginAwareResourceBundleMessageSource {

    public Set getAllKeys(Locale locale) {
        PropertiesHolder pluginProps = getMergedPluginProperties(locale)
        Set pluginSet = pluginProps.getProperties().keySet()
        PropertiesHolder props = getMergedProperties(locale)
        Set propsSet = props.getProperties().keySet()
        Set returnSet = [] as Set
        returnSet.addAll(propsSet)
        returnSet.addAll(pluginSet)
        return returnSet
    }

    public List<PropertiesHolder> getAllProperties(Locale locale) {
        List<PropertiesHolder> holders = []
        holders << getMergedProperties(locale)
        holders << getMergedPluginProperties(locale)
        holders
    }

}