# Development Dockerfile
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application source code
COPY . .

# Expose port 3002 for Vite development server
EXPOSE 5603

# Start the development server.
# Call Vite directly to avoid broken npm shim resolution in some environments.
CMD ["node", "node_modules/vite/bin/vite.js", "--host", "0.0.0.0", "--port", "5603"]
