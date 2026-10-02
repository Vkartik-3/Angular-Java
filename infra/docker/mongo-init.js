// Runs once against an empty "product" database (mongo's docker-entrypoint-initdb.d
// convention — mirrors infra/docker/postgres-init.sql). Seeds the same catalogue V2__seed_classic_rock.sql
// used to, translated into the category + attributes document shape.
db.products.insertMany([
    { category: "vinyl", price: 36.99, imageUrl: "/assets/products/pf-animals.jpg", attributes: { artist: "Pink Floyd", title: "Animals" }, createdAt: new Date() },
    { category: "vinyl", price: 31.99, imageUrl: "/assets/products/black-sabbath-vol4.jpg", attributes: { artist: "Black Sabbath", title: "Vol. 4" }, createdAt: new Date() },
    { category: "vinyl", price: 38.99, imageUrl: "/assets/products/beatles-abbey-road.jpg", attributes: { artist: "The Beatles", title: "Abbey Road" }, createdAt: new Date() },
    { category: "vinyl", price: 34.99, imageUrl: "/assets/products/led-zeppelin-iv.jpg", attributes: { artist: "Led Zeppelin", title: "IV" }, createdAt: new Date() },
    { category: "vinyl", price: 39.99, imageUrl: "/assets/products/king-crimson-in-the-court.jpg", attributes: { artist: "King Crimson", title: "In the Court of the Crimson King" }, createdAt: new Date() },
    { category: "vinyl", price: 29.99, imageUrl: "/assets/products/acdc-let-there-be-rock.jpg", attributes: { artist: "AC/DC", title: "Let There Be Rock" }, createdAt: new Date() },
    { category: "vinyl", price: 32.99, imageUrl: "/assets/products/thin-lizzy-jailbreak.jpg", attributes: { artist: "Thin Lizzy", title: "Jailbreak" }, createdAt: new Date() },
    { category: "vinyl", price: 33.99, imageUrl: "/assets/products/deep-purple-machine-head.jpg", attributes: { artist: "Deep Purple", title: "Machine Head" }, createdAt: new Date() },
    { category: "vinyl", price: 35.99, imageUrl: "/assets/products/rolling-stones-sticky-fingers.jpg", attributes: { artist: "Rolling Stones", title: "Sticky Fingers" }, createdAt: new Date() },
    { category: "vinyl", price: 33.99, imageUrl: "/assets/products/dire-straits-making-movies.jpg", attributes: { artist: "Dire Straits", title: "Making Movies" }, createdAt: new Date() }
]);