package coach.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiErrorsTest {
    @Test
    fun statusCodesBecomeTypedErrors() {
        assertEquals(LoadError.Network(null), ApiErrors.of(null, ApiCall.PANEL))
        assertEquals(LoadError.Unauthorized, ApiErrors.of(401, ApiCall.PANEL))
        assertEquals(LoadError.Unauthorized, ApiErrors.of(401, ApiCall.LOGIN))
        assertEquals(LoadError.WrongPassword, ApiErrors.of(401, ApiCall.REGISTER))
        assertEquals(LoadError.NoPasskey, ApiErrors.of(404, ApiCall.LOGIN_OPTIONS))
        assertEquals(LoadError.TooManyAttempts, ApiErrors.of(429, ApiCall.REGISTER))
        assertEquals(LoadError.PasskeyRejected, ApiErrors.of(400, ApiCall.REGISTER_VERIFY))
        assertEquals(LoadError.Network(503), ApiErrors.of(503, ApiCall.PANEL))
    }

    @Test
    fun browserPasskeyErrorsBecomeTypedErrors() {
        assertEquals(LoadError.PasskeyCancelled, ApiErrors.ofPasskey("NotAllowedError"))
        assertEquals(LoadError.PasskeyCancelled, ApiErrors.ofPasskey("AbortError"))
        assertEquals(LoadError.NoPasskeySupport, ApiErrors.ofPasskey("NotSupportedError"))
        assertEquals(LoadError.PasskeyRejected, ApiErrors.ofPasskey("InvalidStateError"))
    }
}
