DO $$
DECLARE
    constraint_row record;
BEGIN
    IF to_regclass('public.app_users') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE public.app_users
        ALTER COLUMN password_hash DROP NOT NULL;

    ALTER TABLE public.app_users
        ADD COLUMN IF NOT EXISTS password_change_required boolean NOT NULL DEFAULT false,
        ADD COLUMN IF NOT EXISTS temporary_password_expires_at timestamp;

    FOR constraint_row IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.app_users'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE public.app_users DROP CONSTRAINT %I', constraint_row.conname);
    END LOOP;

    ALTER TABLE public.app_users
        ADD CONSTRAINT app_users_status_check
        CHECK (status IN (
            'ACTIVE',
            'PENDING_ACTIVATION',
            'PENDING_INVITATION',
            'INACTIVE',
            'SUSPENDED'
        ));

    CREATE TABLE IF NOT EXISTS signup_verification_codes (
        id uuid PRIMARY KEY,
        created_at timestamp NOT NULL,
        updated_at timestamp NOT NULL,
        user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
        code_hash varchar(64) NOT NULL,
        expires_at timestamp NOT NULL,
        verified_at timestamp,
        failed_attempts integer NOT NULL DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_signup_codes_user
        ON signup_verification_codes(user_id, created_at DESC);
END $$;
