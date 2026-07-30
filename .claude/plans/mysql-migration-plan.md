# MySQL Migration Plan: H2 → MySQL

## Overview
Replace H2 in-memory database with MySQL server while maintaining existing layered architecture (Controller → Service → Repository).

## Phase 1: Dependencies
- **Add MySQL Connector**: Add `mysql-connector-java` to `pom.xml`
- **Remove H2 runtime**: Delete H2 runtime dependency from `pom.xml`

## Phase 2: Configuration
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shopeasy?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## Phase 3: Schema & Entities
- Convert POJOs to MySQL-compatible JPA entities
- Create `schema.sql` in `src/main/resources` with MySQL DDL
- Implement repositories extending `JpaRepository<Entity, ID>`

## Phase 4: Data Migration
- Generate `data.sql` to populate initial product/user records
- Migrate hardcoded data from H2 to MySQL

## Phase 5: Controller Updates
- Inject repositories/services where needed
- Replace hardcoded product lists with service calls

## Phase 6: Testing
- Run integration tests with MySQL
- Verify application starts and all endpoints return expected data

## Progress Tracker
- [x] Phase 1: Dependencies updated (`pom.xml`)
- [x] Phase 2: Configuration updated (`application.properties`)
- [x] Phase 3: Entities & repositories created (`Product`, `ProductRepository`)
- [x] Phase 4: SQL scripts created (`schema.sql`, `data.sql`)
- [ ] Phase 5: Controller updates (in progress)
- [ ] Phase 6: Testing (pending)