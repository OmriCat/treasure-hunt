#set page("a4",
	margin: 1cm,
)

#let titles = csv("titles.csv").flatten().map(str)

#let qr(title) = align(center)[
	#title \
	#box(image(title + ".svg", height: 90%))
]

#grid(
	columns: (50%, 50%),
	rows:(19%),
	gutter: 4pt,
	inset: 8pt,
	stroke: (paint: gray, dash: "dashed"),
	..titles.map(qr)
)