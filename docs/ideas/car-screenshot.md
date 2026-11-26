# Car screenshot

A PNG of exactly what the car shows, taken only when the owner asks:

- the connection notification's **Screenshot** action (there only while the car shows MyPlay's screen);
- a car button or a home pad direction set to **Car screenshot** (on none by default);
- the Home button in the phone app while connected.

How: for one frame the car display draws into an `ImageReader` instead of the video encoder, then
goes back to it, so the car misses a single frame (at most 250 ms if no frame comes). The frame
is cropped to the car's visible area (the video minus its margins) and saved to
`Pictures/MyPlay/MyPlay-car-yyyyMMdd-HHmmss.png` through MediaStore, with no permission (Android 10
and newer; older phones get a toast and nothing is saved). A toast on the phone and a short notice
on the car say it was saved.

**Privacy (CS-2):** a screenshot holds whatever the car shows at that moment: a message notice, a
caller's name, a destination on the map. It is saved in the phone's shared Pictures, where gallery
apps and any app allowed to see photos can read it. Nothing is ever taken on its own, nothing is
sent anywhere, and nothing about it goes in the log; delete a picture as any other photo.
