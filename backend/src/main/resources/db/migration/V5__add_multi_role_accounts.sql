DO $$
BEGIN
    -- On a brand-new installation Hibernate creates the domain schema after Flyway.
    -- Existing installations already have app_users and need their current role migrated.
    IF to_regclass('public.app_users') IS NULL THEN
        RETURN;
    END IF;

    CREATE TABLE IF NOT EXISTS user_roles (
        user_id UUID NOT NULL,
        role VARCHAR(64) NOT NULL,
        CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
        CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
    );

    INSERT INTO user_roles (user_id, role)
    SELECT id, role
    FROM app_users
    WHERE role IS NOT NULL
    ON CONFLICT (user_id, role) DO NOTHING;

    CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);
END $$;
