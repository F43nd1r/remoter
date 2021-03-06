package remoter.compiler.builder;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import remoter.RemoterProxy;

/**
 * A {@link RemoteBuilder} that knows how to build the proxy and stub classes.
 * This uses other builders internally to build the fields and methods.
 */
class ClassBuilder extends RemoteBuilder {

    static final String PROXY_SUFFIX = "_Proxy";
    static final String STUB_SUFFIX = "_Stub";

    protected ClassBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    public JavaFile.Builder buildProxyClass() {
        ClassName proxyClassName = getProxyClassName();

        TypeSpec.Builder proxyClassBuilder = TypeSpec
                .classBuilder(proxyClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(RemoterProxy.class))
                .addSuperinterface(TypeName.get(getRemoterInterfaceElement().asType()));


        for (TypeParameterElement typeParameterElement : ((TypeElement) getRemoterInterfaceElement()).getTypeParameters()) {
            proxyClassBuilder.addTypeVariable(TypeVariableName.get(typeParameterElement.toString()));
        }

        //constructor
        proxyClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Initialize this {@link " + getProxyClassName().simpleName() + "} with the given {@link IBinder}\n\n")
                .addJavadoc("@param binder An {@link IBinder} that exposes a remote {@link " + getRemoterInterfaceClassName() + "}\n")
                .addParameter(ClassName.get("android.os", "IBinder"), "binder")
                .addStatement("this.mRemote = binder")
                .addStatement("linkToDeath(mDeathRecipient)")
                .addStatement("this._binderID = __getStubID()")
                .build());


        getBindingManager().getFieldBuilder(getRemoterInterfaceElement()).addProxyFields(proxyClassBuilder);
        getBindingManager().getMethoddBuilder(getRemoterInterfaceElement()).addProxyMethods(proxyClassBuilder);

        proxyClassBuilder.addJavadoc("Wraps a remote {@link IBinder} that implements {@link " + getRemoterInterfaceClassName() + "} interface\n");
        proxyClassBuilder.addJavadoc("<p>\n");
        proxyClassBuilder.addJavadoc("Autogenerated by <a href=\"https://bit.ly/Remoter\">Remoter</a>\n");
        proxyClassBuilder.addJavadoc("@see " + getStubClassName().simpleName() + "\n");

        return JavaFile.builder(proxyClassName.packageName(), proxyClassBuilder.build());
    }


    public JavaFile.Builder buildStubClass() {
        ClassName stubClassName = getStubClassName();

        TypeSpec.Builder stubClassBuilder = TypeSpec
                .classBuilder(stubClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(getBindingManager().getType("android.os.Binder")));

        for (TypeParameterElement typeParameterElement : ((TypeElement) getRemoterInterfaceElement()).getTypeParameters()) {
            stubClassBuilder.addTypeVariable(TypeVariableName.get(typeParameterElement.toString()));
        }


        //constructor
        stubClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Initialize this {@link " + getStubClassName().simpleName() + "} with the given {@link " + getRemoterInterfaceClassName() + "} implementation \n\n")
                .addJavadoc("@param serviceImpl An implementation of {@link " + getRemoterInterfaceClassName() + "}\n")
                .addParameter(TypeName.get(getRemoterInterfaceElement().asType()), "serviceImpl")
                .addStatement("this.serviceImpl = serviceImpl")
                .beginControlFlow("this.attachInterface(new $T()", ClassName.get("android.os", "IInterface"))
                .beginControlFlow("public $T asBinder()", ClassName.get("android.os", "IBinder"))
                .addStatement("return " + stubClassName.simpleName() + ".this")
                .endControlFlow()
                .endControlFlow()
                .addStatement(", DESCRIPTOR)")
                .build());


        getBindingManager().getFieldBuilder(getRemoterInterfaceElement()).addStubFields(stubClassBuilder);
        getBindingManager().getMethoddBuilder(getRemoterInterfaceElement()).addStubMethods(stubClassBuilder);

        stubClassBuilder.addJavadoc("Wraps a {@link " + getRemoterInterfaceClassName() + "} implementation and expose it as a remote {@link IBinder}\n");
        stubClassBuilder.addJavadoc("<p>\n");
        stubClassBuilder.addJavadoc("Autogenerated by <a href=\"https://bit.ly/Remoter\">Remoter</a>\n");
        stubClassBuilder.addJavadoc("@see " + getProxyClassName().simpleName() + "\n");

        return JavaFile.builder(stubClassName.packageName(), stubClassBuilder.build());
    }

    private ClassName getStubClassName() {
        return ClassName.get(getRemoterInterfacePackageName(), getRemoterInterfaceClassName() + STUB_SUFFIX);
    }

    private ClassName getProxyClassName() {
        return ClassName.get(getRemoterInterfacePackageName(), getRemoterInterfaceClassName() + PROXY_SUFFIX);
    }

}
