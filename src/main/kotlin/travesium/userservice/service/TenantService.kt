package travesium.userservice.service

import org.springframework.stereotype.Service

/**
 * @author Maja Razinger
 */
@Service
//@("auth")
class TenantService {

    companion object {
        private val tenantContext = ThreadLocal<String>()
    }

    fun setCurrentTenant(tenantId: String?) {
        if (tenantId != null) {
            tenantContext.set(tenantId)
        } else {
            tenantContext.remove()
        }
    }

    fun getCurrentTenant(): String = tenantContext.get() ?: "default"

    fun clear() {
        tenantContext.remove()
    }



}