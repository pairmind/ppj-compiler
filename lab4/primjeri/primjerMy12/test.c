int bar(int x);
int foo(int a, int b) {
	if (a > 0) {
		return a + b;
	} else {
		return 0;
	}
}
int bar(int a) {
	return foo(a, a-1);
}
int main(void) {
	return bar(0)
}