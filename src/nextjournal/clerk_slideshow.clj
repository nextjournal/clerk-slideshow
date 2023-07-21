;; # 🎠 Clerk Slideshow
;; ---
(ns nextjournal.clerk-slideshow
  (:require [nextjournal.clerk.viewer :as v]
            [nextjournal.clerk :as clerk]))

;; With a custom viewer and some helper functions, we can turn a Clerk notebooks into a presentation.
;;
;; `slide-viewer` wraps a collection of blocks into markup suitable for rendering a slide.

(def slide-viewer
  {:render-fn '(fn [blocks opts]
                 [:div.flex.flex-col.justify-center
                  {:style {:min-block-size "100vh"}}
                  (into [:div.text-xl.p-20 {:class ["prose max-w-none prose-h1:mb-0 prose-h2:mb-8 prose-h3:mb-8 prose-h4:mb-8"
                                                    "prose-h1:text-6xl prose-h2:text-5xl prose-h3:text-3xl prose-h4:text-2xl"]}]
                        (nextjournal.clerk.render/inspect-children opts)
                        blocks)])})

;; We need a simpler code viewer than the default one, one that adapts to the full width of the slideshow.
(def code-viewer
  {:render-fn '(fn [code] [:div.code-viewer [nextjournal.clerk.render.code/render-code code {:language "clojure"}]])
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
         :render-fn '(fn [slides]
                       (reagent.core/with-let [!state (reagent.core/atom {:current-slide 0
                                                                          :grid? false
                                                                          :viewport-width js/innerWidth
                                                                          :viewport-height js/innerHeight})
                                               ref-fn (fn [el]
                                                        (when el
                                                          (swap! !state assoc :stage-el el)
                                                          (js/addEventListener "resize"
                                                                               #(swap! !state assoc
                                                                                       :viewport-width js/innerWidth
                                                                                       :viewport-height js/innerHeight))
                                                          (js/document.addEventListener "keydown"
                                                                                        (fn [e]
                                                                                          (case (.-key e)
                                                                                            "Escape" (swap! !state update :grid? not)
                                                                                            "ArrowRight" (when-not (:grid? !state)
                                                                                                           (swap! !state update :current-slide #(min (dec (count slides)) (inc %))))
                                                                                            "ArrowLeft" (when-not (:grid? !state)
                                                                                                          (swap! !state update :current-slide #(max 0 (dec %))))
                                                                                            nil)))))
                                               default-transition {:type :spring :duration 0.4 :bounce 0.1}]
                         (let [{:keys [grid? current-slide viewport-width viewport-height]} @!state]
                           [:div.overflow-hidden.relative.bg-slate-50.dark:bg-slate-800
                            {:ref ref-fn :id "stage" :style {:width viewport-width :height viewport-height}}
                            (into [:> (.. framer-motion -motion -div)
                                   {:style {:width (if grid? viewport-width (* (count slides) viewport-width))}
                                    :initial false
                                    :animate {:x (if grid? 0 (* -1 current-slide viewport-width))}
                                    :transition default-transition}]
                                  (map-indexed
                                   (fn [i slide]
                                     (let [width 250
                                           height 150
                                           gap 40
                                           slides-per-row (int (/ viewport-width (+ gap width)))
                                           col (mod i slides-per-row)
                                           row (int (/ i slides-per-row))]
                                       [:> (.. framer-motion -motion -div)
                                        {:initial false
                                         :class ["absolute left-0 top-0 overflow-x-hidden bg-white"
                                                 (when grid?
                                                   "rounded-lg shadow-lg overflow-y-hidden cursor-pointer ring-1 ring-slate-200 hover:ring hover:ring-blue-500/50 active:ring-blue-500")]
                                         :animate {:width (if grid? width viewport-width)
                                                   :height (if grid? height viewport-height)
                                                   :x (if grid? (+ gap (* (+ gap width) col)) (* i viewport-width))
                                                   :y (if grid? (+ gap (* (+ gap height) row)) 0)}
                                         :transition default-transition
                                         :on-click #(when grid? (swap! !state assoc :current-slide i :grid? false))}
                                        [:> (.. framer-motion -motion -div)
                                         {:style {:width viewport-width
                                                  :height viewport-height
                                                  :transformOrigin "left top"}
                                          :initial false
                                          :animate {:scale (if grid? (/ width viewport-width) 1)}
                                          :transition default-transition}
                                         [nextjournal.clerk.render/inspect-presented slide]]]))
                                   slides))])))))

(comment
  (clerk/add-viewers! [viewer])
  (clerk/reset-viewers! clerk/default-viewers))
