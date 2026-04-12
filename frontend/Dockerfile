# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Set build argument for API URL with default value
ARG VITE_API_URL=http://localhost:8080/api

# Debug: print environment variable
RUN echo "构建时VITE_API_URL环境变量值为: ${VITE_API_URL}"

# Set as environment variable for Vite build
ENV VITE_API_URL=${VITE_API_URL}

# Build application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration template
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Set default port
ENV PORT=80

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]