package com.axguard.sdk.api.models.threats

/**
 * An HTTP proxy is configured, which may indicate traffic interception.
 * [HttpProxy.host] and [PacProxy.scriptUrl] may embed identifying data.
 */
public interface ProxyThreat : SecurityCheckResult.ThreatDetected {

    /**
     * The default HTTP proxy, if set.
     */
    public val http: HttpProxy?

    /**
     * The default PAC (proxy auto-config) proxy, if set.
     */
    public val pac: PacProxy?

    /**
     * At least one network defines its own proxy.
     */
    public val perNetworkProxyPresent: Boolean

    /**
     * An http(s).proxyHost JVM system property is set.
     */
    public val proxySystemPropertySet: Boolean

    /**
     * A host:port HTTP proxy.
     */
    public interface HttpProxy {
        /**
         * Proxy host.
         */
        public val host: String

        /**
         * Proxy port.
         */
        public val port: Int
    }

    /**
     * A proxy auto-config (PAC) script.
     */
    public interface PacProxy {
        /**
         * URL of the PAC script.
         */
        public val scriptUrl: String
    }
}
