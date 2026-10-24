// ============================================================
//  Retail Management System — MongoDB seed (reviews)
//  Flexible / unstructured data lives in MongoDB.
//  Run:  mongosh "mongodb://localhost:27017/reviews" db/mongo_seed.js
// ============================================================

db = db.getSiblingDB("reviews");
db.reviews.drop();

// storeIds that exist in MySQL, productIds 1..15, customerIds 1..8
const storeIds    = [1, 2, 3, 5, 7, 8, 10];
const productIds  = Array.from({ length: 15 }, (_, i) => i + 1);
const customerIds = [1, 2, 3, 4, 5, 6, 7, 8];

const positive = [
  "The quality exceeded my expectations.",
  "Absolutely love it — works flawlessly.",
  "Great value for the price, highly recommend.",
  "Fast delivery and the product feels premium.",
  "Exactly as described. Very happy with the purchase.",
  "Fantastic build quality and design.",
  "Best purchase I've made this year!",
  "Setup was easy and it performs brilliantly."
];
const neutral = [
  "Does the job, nothing extraordinary.",
  "Good product but the packaging could be better.",
  "It's fine for everyday use.",
  "Decent quality, met my basic needs.",
  "Works as expected, no complaints so far."
];
const negative = [
  "Stopped working after a few weeks.",
  "Not worth the price in my opinion.",
  "Arrived with a minor scratch.",
  "Battery life is shorter than advertised.",
  "Customer support was slow to respond."
];

function pick(arr, i) { return arr[i % arr.length]; }

const docs = [];
let n = 0;
for (const storeId of storeIds) {
  for (const productId of productIds) {
    // ~2 reviews per product/store combination
    const count = ((productId + storeId) % 3); // 0,1 or 2 reviews
    for (let k = 0; k < count; k++) {
      const rating = ((productId + storeId + k) % 5) + 1; // 1..5
      let pool;
      if (rating >= 4)      pool = positive;
      else if (rating === 3) pool = neutral;
      else                   pool = negative;
      docs.push({
        productId: productId,
        customerId: pick(customerIds, n),
        storeId: storeId,
        rating: rating,
        comment: pick(pool, n)
      });
      n++;
    }
  }
}

db.reviews.insertMany(docs);

print("Reviews inserted: " + db.reviews.countDocuments());
print("Sample:");
printjson(db.reviews.find().limit(3).toArray());
