package fyp_grading_platform.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StudentSchemaCompatibility {
    @Bean
    CommandLineRunner detachLegacyStudentAccounts(JdbcTemplate jdbc) {
        return args -> {
            Boolean legacyUserColumn = jdbc.queryForObject("""
                    select exists (
                        select 1
                        from information_schema.columns
                        where table_schema = current_schema()
                          and table_name = 'student_profiles'
                          and column_name = 'user_id'
                    )
                    """, Boolean.class);
            if (!Boolean.TRUE.equals(legacyUserColumn)) return;

            jdbc.update("""
                    update student_profiles sp
                    set first_name = coalesce(sp.first_name, split_part(u.full_name, ' ', 1)),
                        last_name = coalesce(sp.last_name, nullif(trim(substring(u.full_name from position(' ' in u.full_name) + 1)), ''))
                    from app_users u
                    where sp.user_id = u.id
                    """);
            jdbc.execute("alter table student_profiles alter column user_id drop not null");
            jdbc.update("""
                    update student_profiles sp
                    set user_id = null
                    from app_users u
                    where sp.user_id = u.id
                      and u.role = 'STUDENT'
                    """);
            jdbc.update("delete from app_users where role = 'STUDENT'");
        };
    }
}
