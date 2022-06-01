;; # 🎠 Clerk Slideshow
^{:nextjournal.clerk/visibility :hide-ns}
(ns nextjournal.clerk-slideshow
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]
            [clojure.walk :as w]))

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

;; We can then override Clerk’s default notebook viewer with
;; a custom slideshow viewer which can be required and set using
;; `clerk/add-viewers!`.
(def viewer
  {:name :clerk/notebook
   :transform-fn (comp v/mark-presented
                   (v/update-val (comp (partial w/postwalk (v/when-wrapped v/inspect-wrapped-value))
                                   doc->slides)))
   :render-fn '(fn [slides]
                 (v/html
                   (reagent/with-let [!state (reagent/atom {:current-slide 0
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
                       [:div.overflow-hidden.relative.bg-slate-50
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
                                  slide]]))
                            slides))]))))})
