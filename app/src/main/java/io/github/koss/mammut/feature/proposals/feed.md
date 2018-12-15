# Feed

The Mastodon feed is a key part of Mammut - this proposal outlines the technical direction to be taken with it.

## Requirements
* Omnidirectional paging
* Streaming
* Must be able to retain paging position across configuration change

## Implementation
The main paging and data maintenance is to be done by a dedicated feed repository. This will be generic enough to accept 
streaming and paging endpoints that return any model, so long as they behave in the same way, i.e notifications.

### The recyclerview
* Each `onBindViewHolder` callback will call out to the feed repository notifying it to load around the specified ID
* The repository will expose an `unseenCount` LiveData instance to be observed by clients of the repo