FROM nginx:1.27-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy production reverse proxy config
COPY nginx.conf /etc/nginx/nginx.conf

# Health check probe
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1/ || exit 1

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
