#!/bin/bash
# init.sh - Development environment setup for logistics-analytics-dashboard
# Based on Anthropic's "effective harnesses for long-running agents" principles

set -e  # Exit on error

echo "=== Logistics Analytics Dashboard Development Environment Setup ==="
echo ""

# Check prerequisites
echo "Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "Error: Java 17 is required. Please install OpenJDK 17."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Warning: Maven not found. Backend build may fail."
fi

if ! command -v node &> /dev/null; then
    echo "Warning: Node.js not found. Frontend development may fail."
fi

if ! command -v docker &> /dev/null; then
    echo "Warning: Docker not found. Containerized deployment may fail."
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Warning: docker-compose not found. Local development may be limited."
fi

echo ""
echo "Prerequisites check completed."
echo ""

# Display project structure
echo "Project structure:"
echo "  backend/     - Spring Boot application"
echo "  frontend/    - Vue 3 application"
echo "  database/    - SQL scripts and migrations"
echo "  docker/      - Docker configurations"
echo ""

# Check for required environment variables
echo "Checking environment variables..."
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Warning: OPENAI_API_KEY not set. Natural Language Query functionality will be limited."
fi

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "1. Review features.json for development tasks"
echo "2. Check claude-progress.txt for session history"
echo "3. Begin with Phase 1 (backend-core) implementation"
echo ""
echo "For development:"
echo "- Backend: cd backend && mvn spring-boot:run"
echo "- Frontend: cd frontend && npm run dev"
echo "- Full stack: docker-compose up"
echo ""

# Create a simple .env template if it doesn't exist
if [ ! -f .env.example ]; then
    cat > .env.example << 'EOF'
# Environment variables for Logistics Analytics Dashboard
OPENAI_API_KEY=your_openai_api_key_here
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/logistics
SPRING_DATASOURCE_USERNAME=reader
SPRING_DATASOURCE_PASSWORD=readonly
SPRING_PROFILES_ACTIVE=development
EOF
    echo "Created .env.example file"
fi