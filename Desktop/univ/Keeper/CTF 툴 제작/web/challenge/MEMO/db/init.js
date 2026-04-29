db = db.getSiblingDB('mydb');

db.createCollection('users', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['username', 'password', 'name', 'role'],
            properties: {
                username: { bsonType: 'string', description: 'unique username' },
                password: { bsonType: 'string', description: 'plaintext or hashed password' },
                name: { bsonType: 'string', description: 'user name' },
                role: { bsonType: 'string', description: 'user role', enum: ['user', 'admin'] }
            }
        }
    },
    validationLevel: 'moderate'
});
db.users.createIndex({ username: 1 }, { unique: true });

db.createCollection('memos', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['title', 'content', 'author', 'views'],
            properties: {
                title: { bsonType: 'string', description: 'memo title' },
                content: { bsonType: 'string', description: 'memo content' },
                author: { bsonType: 'objectId', description: 'user _id' },
                views: { bsonType: 'number', description: 'number of views' },
                sharedKey: { bsonType: 'string', description: 'shared key' }
            }
        }
    },
    validationLevel: 'moderate'
});
db.memos.createIndex({ author: 1, views: -1 });
db.memos.createIndex({ sharedKey: 1 }, { unique: true, sparse: true });

db.users.updateOne(
    { username: 'admin' },
    {
        $set: {
            username: 'admin',
            password: '$2b$10$ATKmwwdDVc6vYPa5MzAC0.WJMwxYFMKNVMVgGnPsHhdVAOwLlBOea',
            name: 'Administrator',
            role: 'admin',
        }
    },
    { upsert: true }
);