var AnimationEngine =/*function()*/{
	setupCanvas: function(canvas) {
		this.canvas = canvas;
		this.updateCanvas(canvas);
		window.addEventListener('resize', this.listener, false);
		this.alive = true;
		//console.log(this);
		this.framerate = 20;
		this.baseColor = 0xFFFF4400;
		this.maxLineDistance = 300;
		this.particleCount = 50;
		this.speed = 5;

		this.particles = [];
		for (var i = 0; i < this.particleCount; i++) this.particles[i] = this.createParticle(Math.random() * this.canvas.width, Math.random() * this.canvas.height, this.speed);

		this.graphics = this.canvas.getContext('2d');
		window.setTimeout(function() { AnimationEngine.onTimeout() }, 1);
	}
	,
	updateCanvas: function() {
		for (var i = 0; i < this.particleCount; i++) this.particles[i].rescale(1 / this.canvas.width, 1 / this.canvas.height);
		this.canvas.width = window.innerWidth;//canvas.clientWidth;
		this.canvas.height = window.innerHeight;//canvas.clientHeight;
		this.canvas.style.width = '100%';
		this.canvas.style.height = '100%';
		console.log("resize: " + this.canvas.width + " " + this.canvas.height);
		for (var i = 0; i < this.particleCount; i++) this.particles[i].rescale(this.canvas.width, this.canvas.height);
	}
	,
	listener: function() {
		//console.log(this);
		AnimationEngine.updateCanvas();
	}
	,
	onTimeout: function() {
		window.setTimeout(function() { AnimationEngine.onTimeout() }, 1000 / this.framerate);
		this.onDraw(this.canvas.width, this.canvas.height);
	}
	,
	onDraw: function(width, height) {
		this.graphics.clearRect(0, 0, width, height);
		for (var i = 0; i < this.particleCount; i++) this.particles[i].update(width, height);
		for (var i = 0; i < this.particleCount; i++) {
			for (var j = 0; j < this.particleCount; j++) {
				if (i == j) continue;
				let brightness = this.maxLineDistance - this.getDistanceOfParticle(this.particles[i], this.particles[j]);
				if (brightness < 0) continue;
				brightness = brightness / this.maxLineDistance * 256;
				if (brightness > 128) {
					brightness = 255;
				} else {
					brightness = brightness * 2;
				}
				brightness = (this.baseColor & 0x00FFFFFF) | ((Math.round(Math.min(Math.max(brightness, 0), 255)) & 0xFF) << 24);
				this.drawLine(this.particles[i].x, this.particles[i].y, this.particles[j].x, this.particles[j].y, brightness);
			}
		}
	}
	,
	getDistanceOfParticle: function(p1, p2) {
		let x = p2.x - p1.x;
		let y = p2.y - p1.y;
		return Math.sqrt(x * x + y * y);
		//return (x*x + y*y);
	}
	,
	drawLine: function(x, y, w, h) {
		this.graphics.beginPath();
		this.graphics.rect(x, y, w, h);
		this.graphics.fillStyle = "green";
		this.graphics.fill();
		this.graphics.closePath();
	}
	,
	drawLine: function(p1x, p1y, p2x, p2y, color) {
		let gradient = this.graphics.createLinearGradient(parseInt(p1x), parseInt(p1y), parseInt(p2x), parseInt(p2y));
		let str_colorStyle = 'rgba(' + ((color >> 16) & 0xFF) + ',' + ((color >> 8) & 0xFF) + ',' + (color & 0xFF) + ',' + (((color >> 24) & 0xFF) / 256) + ')';
		gradient.addColorStop(0, str_colorStyle);
		gradient.addColorStop(1, str_colorStyle);
		this.graphics.beginPath();
		this.graphics.strokeStyle = gradient;
		this.graphics.moveTo(p1x, p1y);
		this.graphics.lineTo(p2x, p2y);
		this.graphics.stroke();
		this.graphics.fill();
		this.graphics.closePath();
	}
	,
	createParticle: function(x, y, speed) {
		let Particle = {
			init: function(x, y, speed) {
				this.x = x;
				this.y = y;
				this.mx = (Math.random() - 0.5) * 2 * speed;
				this.my = (Math.random() - 0.5) * 2 * speed;
			}
			,
			update: function(w, h) {
				this.x += this.mx;
				this.y += this.my;
				if (this.x < 0) {
					this.x = 0;
					this.mx *= -1;
				}
				if (this.y < 0) {
					this.y = 0;
					this.my *= -1;
				}
				if (this.x >= w) {
					this.x = w - 1;
					this.mx *= -1;
				}
				if (this.y >= h) {
					this.y = h - 1;
					this.my *= -1;
				}
			}
			,
			rescale: function(sx, sy) {
				this.x *= sx;
				this.y *= sy;
				this.mx *= sx;
				this.my *= sy;
			}
		};
		Particle.init(x, y, speed);
		return Particle;
	}
};

function launchAnimation(canvas) {
	AnimationEngine.setupCanvas(canvas);
}