package sionic.user.domain

enum class Role {
    MEMBER,
    ADMIN;

    fun isAdmin(): Boolean = this == ADMIN

    fun canAccessAllResources(): Boolean = isAdmin()
}
