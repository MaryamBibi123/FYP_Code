{
"Users": {
"UserId_123": {
"category":
{
"0": "Electronics",
"1": "Furniture",
"2": "Beauty",
"3": "Clothing",
"4": "Books",

}
"fullName": "John Doe",
"dob": "15/05/1998",
"gender": "Male",
"mobile": "03123456789",
"bids": {
"placed": {
"BidId_1": true
},
"received": {
"BidId_2": true
}
}
},
"UserId_456": {
"fullName": "Jane Smith",
"dob": "20/07/1995",
"gender": "Female",
"mobile": "03211223344",
"bids": {
"received": {
"BidId_1": true
}
}
},
"UserId_789": {
"fullName": "Alice Johnson",
"dob": "10/12/2000",
"gender": "Female",
"mobile": "03332221111",
"bids": {
"placed": {
"BidId_2": true
}
}
}
},
"Items": {
"-OLsfecHM-Y3RMSC9P8C": {
"details": {
"availability": 5,
"category": "Electronics",
"condition": "New",
"description": "Example Description",
"imageUrls": [
"https://firebasestorage.googleapis.com/v0/...",
"https://firebasestorage.googleapis.com/v0/..."
],
"productName": "Example Product"
},
"userId": "UserId_456",
"bids": {
"BidId_1": true,
"BidId_2": true
}
}
},
"Bids": {
"BidId_1": {
"bidderId": "UserId_123",
"receiverId": "UserId_456",
"itemId": "-OLsfecHM-Y3RMSC9P8C",
"offeredItemId": "-XYZ12345",
"timestamp": "2024-03-23T12:00:00Z",
"status": "pending"
},
"BidId_2": {
"bidderId": "UserId_789",
"receiverId": "UserId_456",
"itemId": "-OLsfecHM-Y3RMSC9P8C",
"offeredItemId": "-ABC67890",
"timestamp": "2024-03-23T12:10:00Z",
"status": "pending"
}
}
}
