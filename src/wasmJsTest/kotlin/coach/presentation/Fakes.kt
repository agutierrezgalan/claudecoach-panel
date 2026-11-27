package coach.presentation

import coach.core.Outcome
import coach.data.LoadError
import coach.data.PanelApi
import coach.data.Passkeys
import coach.data.SessionStore
import coach.data.SessionToken
import coach.fixtures.ContractFixtures

/** API de mentira: «buena» es la contraseña de registro y «tok-ok» el único token válido. */
class FakeApi : PanelApi {
    var hasPasskey = true
    var panelResult: Outcome<String, LoadError> = Outcome.Ok(ContractFixtures.PLAIN)
    var tooManyAttempts = false
    val calls = mutableListOf<String>()

    override suspend fun registrationOptions(password: String): Outcome<String, LoadError> {
        calls += "registrationOptions"
        return when {
            tooManyAttempts -> Outcome.Err(LoadError.TooManyAttempts)
            password != "buena" -> Outcome.Err(LoadError.WrongPassword)
            else -> Outcome.Ok("""{"challenge":"reg"}""")
        }
    }

    override suspend fun register(credentialJson: String): Outcome<Unit, LoadError> {
        calls += "register"
        hasPasskey = true
        return Outcome.Ok(Unit)
    }

    override suspend fun loginOptions(): Outcome<String, LoadError> {
        calls += "loginOptions"
        return if (hasPasskey) Outcome.Ok("""{"challenge":"login"}""") else Outcome.Err(LoadError.NoPasskey)
    }

    override suspend fun login(assertionJson: String): Outcome<SessionToken, LoadError> {
        calls += "login"
        return Outcome.Ok(SessionToken("tok-ok"))
    }

    override suspend fun panel(token: SessionToken): Outcome<String, LoadError> {
        calls += "panel"
        return if (token.value == "tok-ok") panelResult else Outcome.Err(LoadError.Unauthorized)
    }

    override suspend fun logout(token: SessionToken) {
        calls += "logout"
    }
}

class FakePasskeys(var cancel: Boolean = false) : Passkeys {
    var creates = 0
    var gets = 0
    override suspend fun create(optionsJson: String): Outcome<String, LoadError> {
        creates++
        return if (cancel) Outcome.Err(LoadError.PasskeyCancelled) else Outcome.Ok("""{"id":"cred"}""")
    }
    override suspend fun get(optionsJson: String): Outcome<String, LoadError> {
        gets++
        return if (cancel) Outcome.Err(LoadError.PasskeyCancelled) else Outcome.Ok("""{"id":"cred"}""")
    }
}

class FakeSessionStore(var stored: SessionToken? = null) : SessionStore {
    var clears = 0
    override suspend fun load(): SessionToken? = stored
    override suspend fun save(token: SessionToken) { stored = token }
    override suspend fun clear() { clears++; stored = null }
}
