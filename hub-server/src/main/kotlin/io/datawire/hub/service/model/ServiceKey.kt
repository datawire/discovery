package io.datawire.hub.service.model

import io.datawire.hub.tenant.model.TenantId
import java.net.URI


data class ServiceKey(val tenant: TenantId, val name: ServiceName, val uri: URI)