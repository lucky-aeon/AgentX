FROM golang:1.23-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o proxy-server

FROM supercorp/supergateway

RUN apk add --update --no-cache git

# Install python/pip
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 py3-pip

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /usr/local/bin/

# Copy the proxy server binary from builder
COPY --from=builder /app/proxy-server /usr/local/bin/

# Add execute permissions and set root user
USER root
RUN chmod +x /usr/local/bin/supergateway && \
    chmod +x /usr/local/bin/proxy-server && \
    chmod +x /usr/local/bin/uvx && \
    chmod +x /usr/local/bin/uv

# Set working directory
WORKDIR /etc/proxy

# Set the proxy server as entrypoint
ENTRYPOINT ["/usr/local/bin/proxy-server"]
