git submodule init
git submodule update

android update project -s --path . --library libs/ActionBarSherlock/library/ -l libs/locale/

android update lib-project --path libs/locale/
android update lib-project --target 7 --path libs/ActionBarSherlock/library/
