package Engine

type engine struct {
	name String
}

func New() *engine {
	return &engine{}
}
