;; # 🎠 Clerk Slideshow
^{:nextjournal.clerk/visibility :hide-ns}
(ns nextjournal.clerk-slideshow
  (:require [clojure.java.io :as io]
            [clojure.walk :as w]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

;; With a custom viewer and some helper functions, we can show Clerk notebooks
;; as slideshow. `->slide` wraps a collection of blocks into markup suitable
;; for rendering a slide.
(defn ->slide [blocks]
  [:div.flex.flex-col.justify-center
   {:style {:min-block-size "100vh"}}
   (into [:div.text-xl.p-20 {:class ["prose max-w-none prose-h1:mb-0 prose-h2:mb-8 rose-h3:mb-8 prose-h4:mb-8"
                                     "prose-h1:text-6xl prose-h2:text-5xl prose-h3:text-3xl prose-h4:text-2xl"]}]
     (map (comp (fn [block] (if (:type block) (v/md block) (v/with-viewer :clerk/result block)))))
     blocks)])

;; The `doc->slides` helper function takes a Clerk notebook and
;; partitions its blocks into slides separated by Markdown rulers (`---`).
(defn doc->slides [{:as doc :keys [blocks]}]
  (sequence (comp (mapcat (partial v/with-block-viewer doc))
                  (mapcat #(if (= :markdown (v/->viewer %)) (-> % v/->value :content) [(v/->value %)]))
                  (partition-by (comp #{:ruler} :type))
                  (remove (comp #{:ruler} :type first))
                  (map ->slide))
            blocks))

^::clerk/no-cache
(clerk/eval-cljs-str
 (slurp (io/resource "nextjournal/clerk_slideshow/viewer.cljs")))

;; We can then override Clerk’s default notebook viewer with
;; a custom slideshow viewer which can be required and set using
;; `clerk/add-viewers!`.
(def viewer
  {:name :clerk/notebook
   :transform-fn (comp v/mark-presented
                       (v/update-val (comp (partial w/postwalk (v/when-wrapped v/inspect-wrapped-value))
                                           doc->slides)))
   :render-fn 'nextjournal.clerk-slideshow-viewer/render-slides})
