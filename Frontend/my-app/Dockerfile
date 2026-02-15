# Build Stage
FROM node:22-alpine as build
WORKDIR /app
COPY package*.json ./
# Update npm to latest version to avoid warnings
RUN npm install -g npm@11.10.0
RUN npm install
COPY . .
RUN npm run build

# Production Stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
