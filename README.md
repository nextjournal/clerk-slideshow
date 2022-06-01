# 🎠 clerk-slideshow

Build your slideshows using Clerk notebooks!

https://user-images.githubusercontent.com/1944/171370671-6a4844ce-389c-4ba4-8b4b-908316b8b9ae.mp4

## How does it work?

Simply require `clerk-slideshow`…

```clojure
(ns simple_slideshow
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk-slideshow :as slideshow]))
```

…and add it to Clerk’s viewers using `clerk/add-viewers!`:

```clojure
(clerk/add-viewers! [slideshow/viewer])
```

With that in place, you can use Markdown comments to write your slides’ content. Use Markdown rulers (`---`) to separate your slides. You can use everything that you’ll normally use in your Clerk notebooks: Markdown, plots, code blocks, you name it.

Press `←` and `→` to navigate between slides or `Escape` to get an overview.
