#!/bin/sh

set -xe

cd public

exec deno run --allow-net --allow-read https://deno.land/std@0.143.0/http/file_server.ts
