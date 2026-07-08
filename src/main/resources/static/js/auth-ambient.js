(function () {
    const canvas = document.querySelector("#ambientCanvas");
    if (!canvas || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const ctx = canvas.getContext("2d");
    const tokens = ["context", "review", "essay", "listen", "speak", "read", "word", "grammar", "story", "phrase", "focus", "fluent"];
    const palette = [
        { core: "rgba(88, 204, 2, 0.18)", glow: "rgba(88, 204, 2, 0.07)", rim: "rgba(88, 204, 2, 0.38)", ink: "rgba(61, 146, 0, 0.68)" },
        { core: "rgba(28, 176, 246, 0.18)", glow: "rgba(28, 176, 246, 0.07)", rim: "rgba(28, 176, 246, 0.36)", ink: "rgba(7, 99, 146, 0.68)" },
        { core: "rgba(255, 200, 0, 0.18)", glow: "rgba(255, 200, 0, 0.07)", rim: "rgba(255, 200, 0, 0.36)", ink: "rgba(106, 82, 0, 0.66)" },
        { core: "rgba(139, 116, 255, 0.14)", glow: "rgba(139, 116, 255, 0.06)", rim: "rgba(139, 116, 255, 0.3)", ink: "rgba(77, 59, 165, 0.62)" }
    ];
    const nodes = [];
    let width = 0;
    let height = 0;
    let ratio = 1;
    let lastFrameTime = 0;

    function resize() {
        ratio = Math.min(window.devicePixelRatio || 1, 2);
        width = window.innerWidth;
        height = window.innerHeight;
        canvas.width = Math.floor(width * ratio);
        canvas.height = Math.floor(height * ratio);
        canvas.style.width = `${width}px`;
        canvas.style.height = `${height}px`;
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        seedNodes();
    }

    function desiredNodeCount() {
        const base = Math.round((width * height) / (width < 760 ? 62000 : 56000));
        return Math.max(width < 760 ? 8 : 15, Math.min(width < 760 ? 16 : 32, base));
    }

    function seedNodes() {
        const target = desiredNodeCount();
        while (nodes.length < target) {
            const node = createNode(nodes.length);
            nodes.push(node);
        }
        nodes.length = target;
        nodes.forEach((node, index) => {
            prepareNode(node, index, false);
            containNode(node);
        });
        resolveCollisions(8);
    }

    function randomSpeed() {
        const min = width < 760 ? 0.2 : 0.28;
        const max = width < 760 ? 0.34 : 0.5;
        return min + Math.random() * (max - min);
    }

    function setRandomVelocity(node) {
        const angle = Math.random() * Math.PI * 2;
        node.speed = randomSpeed();
        node.vx = Math.cos(angle) * node.speed;
        node.vy = Math.sin(angle) * node.speed;
    }

    function createNode(index) {
        const node = {
            x: 0,
            y: 0,
            vx: 0,
            vy: 0,
            speed: 0,
            phase: Math.random() * Math.PI * 2,
            scale: 1,
            theme: palette[index % palette.length],
            token: tokens[index % tokens.length],
            age: 0,
            life: 12,
            bump: 0
        };
        prepareNode(node, index, true);
        return node;
    }

    function prepareNode(node, index, shouldPlace) {
        node.token = tokens[Math.floor(Math.random() * tokens.length)];
        node.theme = palette[(index + Math.floor(Math.random() * palette.length)) % palette.length];
        node.scale = 0.92 + Math.random() * 0.34;
        node.phase = Math.random() * Math.PI * 2;
        node.age = 0;
        node.life = 10 + Math.random() * 8;
        node.bump = 0;
        setRandomVelocity(node);
        if (shouldPlace) {
            placeNode(node);
        }
    }

    function nodeFontSize(node) {
        const base = width < 760 ? 12 : 15;
        return Math.round(base * Math.min(1.14, Math.max(0.94, node.scale)));
    }

    function nodeRadius(node) {
        ctx.save();
        ctx.font = `900 ${nodeFontSize(node)}px Segoe UI, Arial`;
        const textWidth = ctx.measureText(node.token).width;
        ctx.restore();
        const minRadius = (width < 760 ? 34 : 42) * node.scale;
        const textRadius = (textWidth / 2) + (width < 760 ? 18 : 23);
        return Math.max(minRadius, textRadius);
    }

    function collisionRadius(node) {
        return nodeRadius(node) + 8;
    }

    function placeNode(node) {
        const radius = collisionRadius(node);
        let best = null;
        let bestGap = -Infinity;
        for (let attempt = 0; attempt < 120; attempt += 1) {
            const x = radius + Math.random() * Math.max(1, width - radius * 2);
            const y = radius + Math.random() * Math.max(1, height - radius * 2);
            let nearestGap = Infinity;
            const clear = nodes.every((other) => {
                if (other === node) return true;
                const minDistance = radius + collisionRadius(other) + 10;
                const gap = Math.hypot(x - other.x, y - other.y) - minDistance;
                nearestGap = Math.min(nearestGap, gap);
                return gap >= 0;
            });
            if (nearestGap > bestGap) {
                bestGap = nearestGap;
                best = { x, y };
            }
            if (clear) {
                node.x = x;
                node.y = y;
                return;
            }
        }
        node.x = best?.x ?? radius + Math.random() * Math.max(1, width - radius * 2);
        node.y = best?.y ?? radius + Math.random() * Math.max(1, height - radius * 2);
    }

    function containNode(node) {
        const radius = collisionRadius(node);
        if (node.x < radius) {
            node.x = radius;
            node.vx = Math.abs(node.vx);
        } else if (node.x > width - radius) {
            node.x = width - radius;
            node.vx = -Math.abs(node.vx);
        }
        if (node.y < radius) {
            node.y = radius;
            node.vy = Math.abs(node.vy);
        } else if (node.y > height - radius) {
            node.y = height - radius;
            node.vy = -Math.abs(node.vy);
        }
    }

    function limitVelocity(node) {
        const minSpeed = width < 760 ? 0.18 : 0.25;
        const maxSpeed = width < 760 ? 0.4 : 0.56;
        const speed = Math.hypot(node.vx, node.vy);
        if (!speed) {
            setRandomVelocity(node);
            return;
        }
        if (speed < minSpeed) {
            node.vx = (node.vx / speed) * minSpeed;
            node.vy = (node.vy / speed) * minSpeed;
            node.speed = minSpeed;
            return;
        }
        if (speed > maxSpeed) {
            node.vx = (node.vx / speed) * maxSpeed;
            node.vy = (node.vy / speed) * maxSpeed;
            node.speed = maxSpeed;
            return;
        }
        node.speed = speed;
    }

    function resolveCollisions(iterations = 2) {
        for (let pass = 0; pass < iterations; pass += 1) {
            for (let i = 0; i < nodes.length; i += 1) {
                for (let j = i + 1; j < nodes.length; j += 1) {
                    const a = nodes[i];
                    const b = nodes[j];
                    const minDistance = collisionRadius(a) + collisionRadius(b) + 2;
                    let dx = a.x - b.x;
                    let dy = a.y - b.y;
                    let distance = Math.hypot(dx, dy);
                    if (!distance) {
                        dx = 1;
                        dy = 0;
                        distance = 1;
                    }
                    if (distance >= minDistance) continue;

                    const nx = dx / distance;
                    const ny = dy / distance;
                    const overlap = minDistance - distance;
                    a.x += nx * overlap * 0.5;
                    a.y += ny * overlap * 0.5;
                    b.x -= nx * overlap * 0.5;
                    b.y -= ny * overlap * 0.5;

                    const relativeVelocity = ((a.vx - b.vx) * nx) + ((a.vy - b.vy) * ny);
                    if (relativeVelocity < 0) {
                        const impulse = -relativeVelocity * 1.04;
                        a.vx += impulse * nx;
                        a.vy += impulse * ny;
                        b.vx -= impulse * nx;
                        b.vy -= impulse * ny;
                    }
                    a.bump = 1;
                    b.bump = 1;
                    limitVelocity(a);
                    limitVelocity(b);
                    containNode(a);
                    containNode(b);
                }
            }
        }
    }

    function nodeLifeAlpha(node) {
        const fadeIn = Math.min(1, node.age / 1.2);
        const fadeOut = Math.min(1, Math.max(0, (node.life - node.age) / 1.6));
        return Math.max(0, Math.min(fadeIn, fadeOut));
    }

    function respawnNode(node, index) {
        prepareNode(node, index, true);
        resolveCollisions(4);
    }

    function drawNodes(time) {
        const dt = Math.min(2.2, Math.max(0.5, (time - lastFrameTime) / 16.67 || 1));
        lastFrameTime = time;
        for (let i = 0; i < nodes.length; i += 1) {
            const node = nodes[i];
            node.age += dt / 60;
            if (node.age >= node.life) {
                respawnNode(node, i);
            }
            node.x += node.vx * dt;
            node.y += node.vy * dt;
            containNode(node);
        }
        resolveCollisions();
        nodes.forEach(limitVelocity);

        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        nodes.forEach((node) => {
            const lifeAlpha = nodeLifeAlpha(node);
            const pulse = 0.78 + Math.sin(time / 1100 + node.phase) * 0.06;
            const bump = Math.max(0, node.bump || 0);
            const bumpScale = 1 + bump * 0.08;
            const radius = nodeRadius(node) * bumpScale;
            const theme = node.theme || palette[0];
            ctx.save();
            ctx.globalAlpha = lifeAlpha * pulse * (width < 760 ? 0.72 : 0.88);
            if (bump > 0.02) {
                ctx.shadowColor = "rgba(35, 49, 67, 0.12)";
                ctx.shadowBlur = 8;
            }
            const fill = ctx.createRadialGradient(
                node.x - radius * 0.36,
                node.y - radius * 0.42,
                1,
                node.x,
                node.y,
                radius * 1.28
            );
            fill.addColorStop(0, "rgba(255, 255, 255, 0.96)");
            fill.addColorStop(0.2, "rgba(255, 255, 255, 0.5)");
            fill.addColorStop(0.62, theme.core);
            fill.addColorStop(1, theme.glow);
            ctx.fillStyle = fill;
            ctx.strokeStyle = theme.rim;
            ctx.lineWidth = 1.4;
            ctx.beginPath();
            ctx.arc(node.x, node.y, radius, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
            ctx.strokeStyle = "rgba(255, 255, 255, 0.58)";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(node.x - radius * 0.08, node.y - radius * 0.1, radius * 0.67, Math.PI * 1.06, Math.PI * 1.73);
            ctx.stroke();
            ctx.fillStyle = "rgba(255, 255, 255, 0.62)";
            ctx.beginPath();
            ctx.arc(node.x - radius * 0.36, node.y - radius * 0.42, radius * 0.14, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = "rgba(255, 255, 255, 0.26)";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(node.x, node.y, radius - 5, Math.PI * 0.15, Math.PI * 0.8);
            ctx.stroke();
            ctx.fillStyle = theme.ink;
            ctx.font = `900 ${nodeFontSize(node)}px Segoe UI, Arial`;
            ctx.fillText(node.token, node.x, node.y);
            ctx.restore();
            node.bump = bump * 0.86;
        });
        ctx.textAlign = "start";
        ctx.textBaseline = "alphabetic";
    }

    function render(time) {
        ctx.clearRect(0, 0, width, height);
        drawNodes(time);
        window.requestAnimationFrame(render);
    }

    resize();
    window.addEventListener("resize", resize);
    window.requestAnimationFrame(render);
})();
