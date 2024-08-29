;; # 🎠 Clerk Slideshow
;; ---
(ns nextjournal.clerk-slideshow
  (:require [nextjournal.clerk.viewer :as v]
            [nextjournal.clerk-slideshow.render :as-alias render]
            [nextjournal.clerk :as clerk]))

;; With a custom viewer and some helper functions, we can turn a Clerk notebooks into a presentation.
;;
;; `slide-viewer` wraps a collection of blocks into markup suitable for rendering a slide.

(def slide-viewer
  {:render-fn `render/render-slide
   :require-cljs true})

;; We need a simpler code viewer than the default one, one that adapts to the full width of the slideshow.
(def code-viewer
  {:render-fn `render/render-code
   :require-cljs true
   :transform-fn (comp v/mark-presented (v/update-val :text-without-meta))})

;; ---
;; The `doc->slides` helper function takes Clerk notebook data and partitions its blocks into slides by occurrences of markdown rulers.
(defn doc->slides [{:as doc :keys [blocks]}]
  (sequence (comp (mapcat (partial v/with-block-viewer doc))
                  (mapcat #(cond
                            (= `v/markdown-viewer (v/->viewer %)) (map v/md (-> % v/->value :content))
                            (= `v/code-block-viewer (v/->viewer %)) [(assoc % :nextjournal/viewer code-viewer)]
                            :else [%]))
                  (partition-by (comp #{:ruler} :type v/->value))
                  (remove (comp #{:ruler} :type v/->value first))
                  (map (partial v/with-viewer slide-viewer)))
            blocks))
;; ---
;; We can then override Clerk’s default notebook viewer with a custom slideshow viewer which can be required and set using
;; `clerk/add-viewers!`.
(def viewer
  (assoc v/notebook-viewer
         :transform-fn (v/update-val doc->slides)
         :render-fn `render/render-slideshow
         :require-cljs true))

(comment
  (clerk/add-viewers! [viewer])
  (clerk/reset-viewers! clerk/default-viewers))
