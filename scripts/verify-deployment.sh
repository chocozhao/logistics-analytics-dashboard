#!/bin/bash

# Deployment verification script for Logistics Analytics Dashboard
# This script checks that all required configuration files exist and are valid

set -e

echo "🔍 Verifying deployment configuration..."

# Check required files
required_files=(
    "render.yaml"
    "backend/Dockerfile"
    "frontend/Dockerfile"
    "docker-compose.yml"
    ".env.example"
    "backend/src/main/resources/application-prod.properties"
    "scripts/migrate-to-render.sql"
    "database/init.sql"
    "database/sample-data.sql"
)

missing_files=()
for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        missing_files+=("$file")
        echo "❌ Missing: $file"
    else
        echo "✅ Found: $file"
    fi
done

if [ ${#missing_files[@]} -ne 0 ]; then
    echo "❌ Missing required files: ${missing_files[*]}"
    exit 1
fi

# Validate YAML files
echo "📋 Validating YAML files..."

# Check render.yaml structure
if ! grep -q "services:" render.yaml; then
    echo "❌ render.yaml missing 'services:' section"
    exit 1
fi

if ! grep -q "databases:" render.yaml; then
    echo "❌ render.yaml missing 'databases:' section"
    exit 1
fi

echo "✅ render.yaml structure looks good"

# Validate docker-compose.yml
if ! docker-compose config > /dev/null 2>&1; then
    echo "❌ docker-compose.yml validation failed"
    docker-compose config
    exit 1
fi

echo "✅ docker-compose.yml is valid"

# Check Dockerfile syntax
echo "🐳 Checking Dockerfile syntax..."

# Backend Dockerfile
if ! grep -q "FROM openjdk:17-slim" backend/Dockerfile; then
    echo "❌ Backend Dockerfile missing Java base image"
    exit 1
fi

if ! grep -q "COPY --from=builder /app/target/\*.jar app.jar" backend/Dockerfile; then
    echo "⚠️  Backend Dockerfile may not copy JAR correctly"
fi

# Frontend Dockerfile
if ! grep -q "ARG VITE_API_URL" frontend/Dockerfile; then
    echo "❌ Frontend Dockerfile missing VITE_API_URL ARG"
    exit 1
fi

if ! grep -q "RUN VITE_API_URL=\${VITE_API_URL} npm run build" frontend/Dockerfile; then
    echo "⚠️  Frontend Dockerfile may not use VITE_API_URL in build"
fi

echo "✅ Dockerfiles have required components"

# Check production properties
echo "⚙️  Checking production configuration..."

if ! grep -q "cors.allowed-origins=\${CORS_ALLOWED_ORIGINS" backend/src/main/resources/application-prod.properties; then
    echo "❌ application-prod.properties missing CORS configuration"
    exit 1
fi

if ! grep -q "SPRING_PROFILES_ACTIVE=prod" backend/src/main/resources/application-prod.properties; then
    echo "⚠️  application-prod.properties may not set prod profile"
fi

echo "✅ Production configuration looks good"

# Check migration script
echo "🗄️  Checking database migration script..."

if ! grep -q "GRANT SELECT ON ALL TABLES" scripts/migrate-to-render.sql; then
    echo "⚠️  Migration script may not grant proper permissions"
fi

echo "✅ Migration script looks good"

# Check environment template
echo "🔧 Checking environment template..."

if ! grep -q "OPENAI_API_KEY=" .env.example; then
    echo "❌ .env.example missing OPENAI_API_KEY"
    exit 1
fi

if ! grep -q "SPRING_PROFILES_ACTIVE=prod" .env.example; then
    echo "⚠️  .env.example missing production profile hint"
fi

echo "✅ Environment template looks good"

# Summary
echo ""
echo "========================================="
echo "✅ Deployment verification completed successfully!"
echo ""
echo "Next steps:"
echo "1. Push to GitHub"
echo "2. Connect to Render.com"
echo "3. Configure OPENAI_API_KEY in Render dashboard"
echo "4. Render will automatically deploy using render.yaml"
echo ""
echo "Your application will be available at:"
echo "  Frontend: https://logistics-frontend.onrender.com"
echo "  Backend API: https://logistics-backend.onrender.com/api"
echo "========================================="