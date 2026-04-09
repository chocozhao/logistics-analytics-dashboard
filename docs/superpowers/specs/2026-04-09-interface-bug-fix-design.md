---
name: Interface Bug Fix Design
description: Design for fixing interface errors in logistics analytics dashboard
type: project
---

# Interface Bug Fix Design

## Overview
Fix interface errors in the logistics analytics dashboard application. The backend is running in IntelliJ IDEA with errors, and frontend is also running.

## Problem Analysis

Based on code review, the following potential issues were identified:

1. **SQL Compatibility Issues**: Native queries in `OrderRepository.java` use H2-specific functions (`DATEADD`, `DAY_OF_WEEK`, `PARSEDATETIME`, `FORMATDATETIME`) that may not work in PostgreSQL.
2. **Configuration Complexity**: `DatabaseConfig.java` has complex logic and debug outputs that may cause configuration parsing issues.
3. **Data Initialization Conflicts**: Potential conflict between `spring.sql.init.mode=never` in `application-dev.properties` and `DataInitializer.java`.
4. **CORS Configuration**: Possible port mismatch between frontend and backend.

## Design Solution

### Phase 1: Error Diagnosis
- Start the application and collect console error logs
- Test basic API endpoints (`/api/dashboard/kpis/recent`) to verify connectivity
- Identify the root cause from error messages

### Phase 2: SQL Compatibility Fix
**Primary Issue**: Repository queries use H2 functions incompatible with PostgreSQL.

**Solution**: 
- Modify native queries to use PostgreSQL-compatible functions
- Use `DATE_TRUNC()` for date grouping instead of H2-specific functions
- Create database-specific queries or use JPQL where possible

**Example Changes**:
- Replace `DATEADD('DAY', 1 - DAY_OF_WEEK(o.order_date), o.order_date)` with PostgreSQL's `DATE_TRUNC('week', o.order_date)`
- Replace `PARSEDATETIME(FORMATDATETIME(o.order_date, 'yyyy-MM-01'), 'yyyy-MM-dd')` with `DATE_TRUNC('month', o.order_date)`

### Phase 3: Database Configuration Simplification
**Issue**: Complex environment variable parsing in `DatabaseConfig.java`.

**Solution**:
- Simplify configuration logic
- Rely more on Spring Boot auto-configuration
- Remove excessive debug output that may interfere with normal operation

### Phase 4: Data Initialization Verification
**Issue**: Potential conflict between SQL initialization and Java-based initialization.

**Solution**:
- Ensure `DataInitializer` works correctly with `spring.sql.init.mode=never`
- Verify sample data is inserted correctly
- Test that queries return expected results

### Phase 5: CORS and API Testing
**Issue**: Frontend-backend communication issues.

**Solution**:
- Verify CORS configuration in `WebConfig.java` and `@CrossOrigin` annotations
- Test API endpoints from frontend
- Ensure correct API base URL in frontend store

## Implementation Plan

### Step 1: Quick Test
1. Start backend application
2. Capture error logs
3. Test `/api/dashboard/kpis/recent` endpoint

### Step 2: Fix SQL Queries
1. Identify all problematic native queries in `OrderRepository.java`
2. Create PostgreSQL-compatible versions
3. Test with H2 database first, then PostgreSQL

### Step 3: Configuration Cleanup
1. Simplify `DatabaseConfig.java`
2. Verify environment variable parsing
3. Test with different profiles (dev, prod)

### Step 4: Integration Testing
1. Test all dashboard endpoints
2. Verify frontend-backend communication
3. Ensure data displays correctly in charts

## Success Criteria
1. Backend starts without errors
2. All API endpoints return valid responses
3. Frontend successfully fetches and displays data
4. Charts render with sample data
5. Natural language query endpoint works (basic functionality)

## Notes
- Prioritize fixing the actual errors found in logs
- The design may need adjustment based on specific error messages
- Focus on making the application functional first, then optimize