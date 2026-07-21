FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY . /usr/share/nginx/html

RUN chmod +x /docker-entrypoint.sh \
    && rm -f /usr/share/nginx/html/Dockerfile \
            /usr/share/nginx/html/docker-entrypoint.sh \
            /usr/share/nginx/html/nginx.conf \
            /usr/share/nginx/html/vercel.json \
            /usr/share/nginx/html/.dockerignore

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
