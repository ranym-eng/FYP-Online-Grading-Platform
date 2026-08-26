DO $$
DECLARE
    constraint_row record;
BEGIN
    IF to_regclass('public.app_users') IS NULL THEN
        RETURN;
    END IF;

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
            'PENDING_INVITATION',
            'INACTIVE',
            'SUSPENDED'
        ));
END $$;
